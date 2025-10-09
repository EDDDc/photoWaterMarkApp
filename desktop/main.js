const { app, BrowserWindow, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const net = require('net');
const { spawn } = require('child_process');
const waitOn = require('wait-on');
const treeKill = require('tree-kill');

const BACKEND_PORT = Number(process.env.BACKEND_PORT) || 8080;
const BACKEND_HOST = process.env.BACKEND_HOST || '127.0.0.1';
const REPO_ROOT = path.resolve(__dirname, '..');
const BACKEND_DIR = path.join(REPO_ROOT, 'backend');

let backendProcess;
let mainWindow;
let didSpawnBackend = false;

function findBackendJar() {
  const targetDir = path.join(BACKEND_DIR, 'target');
  if (!fs.existsSync(targetDir)) {
    return null;
  }

  const jarFiles = fs
    .readdirSync(targetDir)
    .filter((file) => file.endsWith('.jar') && !file.endsWith('.original'));

  if (jarFiles.length === 0) {
    return null;
  }

  jarFiles.sort((a, b) => fs.statSync(path.join(targetDir, b)).mtimeMs - fs.statSync(path.join(targetDir, a)).mtimeMs);
  return path.join(targetDir, jarFiles[0]);
}

function isPortInUse(host, port) {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    socket.once('error', () => resolve(false));
    socket.connect(port, host, () => {
      socket.end();
      resolve(true);
    });
    socket.setTimeout(1000, () => {
      socket.destroy();
      resolve(false);
    });
  });
}

async function ensureBackend() {
  const portBusy = await isPortInUse(BACKEND_HOST, BACKEND_PORT);
  if (portBusy) {
    return;
  }

  const jarPath = findBackendJar();
  if (!jarPath) {
    const message = 'Backend executable jar not found. Please run ./mvnw clean package -DskipTests inside backend first.';
    dialog.showErrorBox('Backend Not Built', message);
    throw new Error(message);
  }

  didSpawnBackend = true;
  backendProcess = spawn('java', ['-jar', jarPath, `--server.port=${BACKEND_PORT}`], {
    cwd: BACKEND_DIR,
    stdio: 'inherit',
  });

  backendProcess.on('exit', (code) => {
    backendProcess = null;
    if (code !== 0 && mainWindow) {
      dialog.showErrorBox('Backend Stopped', `Spring Boot process exited with status ${code}.`);
    }
  });

  await waitOn({
    resources: [`tcp:${BACKEND_HOST}:${BACKEND_PORT}`],
    timeout: 60000,
    interval: 500,
  });
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    webPreferences: {
      contextIsolation: true,
    },
  });

  const appUrl = `http://${BACKEND_HOST}:${BACKEND_PORT}`;

  const loadUrl = () => {
    if (!mainWindow || mainWindow.isDestroyed()) {
      return;
    }

    mainWindow.loadURL(appUrl).catch((error) => {
      console.error('Failed to load main URL:', error);
    });
  };

  mainWindow.webContents.on('did-fail-load', () => {
    setTimeout(loadUrl, 1000);
  });

  loadUrl();

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

async function startApplication() {
  try {
    await ensureBackend();
    createWindow();
  } catch (error) {
    console.error('Failed to start desktop shell:', error);
    app.quit();
  }
}

function stopBackend() {
  if (backendProcess && didSpawnBackend) {
    try {
      treeKill(backendProcess.pid, 'SIGTERM');
    } catch (error) {
      console.warn('Failed to terminate backend process:', error);
    }
  }
}

app.on('ready', startApplication);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('before-quit', () => {
  stopBackend();
});

app.on('activate', () => {
  if (mainWindow === null) {
    startApplication();
  }
});

process.on('exit', stopBackend);
process.on('SIGINT', () => {
  stopBackend();
  process.exit(0);
});
process.on('SIGTERM', () => {
  stopBackend();
  process.exit(0);
});
