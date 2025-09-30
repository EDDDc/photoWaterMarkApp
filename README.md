# Photo Watermark App

Spring Boot + Vue 3 application for batch applying watermarks to local images on Windows. This repository hosts the full-stack implementation outlined in [docs/PRD.md](docs/PRD.md).

## Project structure

- `backend/` 鈥?Spring Boot 3 service exposing `/api` endpoints.
- `frontend/` 鈥?Vue 3 + TypeScript SPA that consumes the backend.
- `docs/` 鈥?Product requirement document and supplementary design notes.

## Prerequisites

- JDK 17+
- Node.js 20+
- npm (bundled with Node.js)

## Development setup

### 1. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`.

### 2. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Visit `http://localhost:5173` in a Chromium-based browser. Vite proxies `/api/*` calls to the local backend. 鐧诲綍椤甸潰鍖呭惈锛?
- 鍚庣鍋ュ悍妫€娴嬶紙/api/health锛夈€?- 妯℃澘绠＄悊闈㈡澘锛屽彲淇濆瓨/缂栬緫/鍒犻櫎姘村嵃妯℃澘骞跺疄鏃惰鍙栧悗绔暟鎹€?- 瀛椾綋鏋氫妇缁撴灉锛屽彲鐢ㄤ簬鎻愮ず鍓嶇灞曠ず鍝簺瀛椾綋宸茬粡鍦ㄧ郴缁熶腑鍙敤銆?
### 3. Run automated checks

```bash
cd backend
./mvnw test

cd ../frontend
npm run build
```

The frontend build step compiles TypeScript (via `vue-tsc`) and produces production assets. 杩愯瀹屾垚鍚庡彲鍒犻櫎 `frontend/dist/`锛堝凡鍒楀叆 `.gitignore`锛夈€?
## API overview

| Method | Path                   | Description                       |
| ------ | ---------------------- | --------------------------------- |
| GET    | `/api/health`          | 鍋ュ悍妫€鏌ワ紝杩斿洖鏈嶅姟鐘舵€佷笌鏃堕棿鎴炽€?| GET    | `/api/fonts`           | 鍒楀嚭鎿嶄綔绯荤粺鍙敤瀛椾綋锛堟寜鍚嶇О鎺掑簭锛夈€?| GET    | `/api/templates`       | 鑾峰彇宸蹭繚瀛樼殑姘村嵃妯℃澘鍒楄〃銆?| GET    | `/api/templates/{id}`  | 璇诲彇鍗曚釜妯℃澘璇︽儏銆?               |
| POST   | `/api/templates`       | 鏂板缓鎴栨洿鏂版ā鏉匡紙鏍规嵁 `id` 鍒ゆ柇锛夈€倈
| DELETE | `/api/templates/{id}`  | 鍒犻櫎鎸囧畾妯℃澘銆?                   |

璇锋眰/鍝嶅簲閲囩敤 JSON锛屽墠绔ず渚嬫帴鍙ｅ畾涔変綅浜?`frontend/src/services/api.ts`銆?
## Data storage

榛樿瀛樺偍鐩綍閬靛惊锛?
- Windows锛歚%APPDATA%/PhotoWatermark`
- 鍏朵粬绯荤粺锛堝紑鍙戞祴璇曠敤锛夛細`${user.home}/.photo-watermark`

妯℃澘浠?JSON 鏂囦欢褰㈠紡淇濆瓨鍦?`templates/` 瀛愮洰褰曘€備緥濡傦細

```
%APPDATA%/PhotoWatermark/templates/{templateId}.json
```

鑻ラ渶瑕佽嚜瀹氫箟鐩綍锛屽彲鍦?`backend/src/main/resources/application.properties` 涓缃細

```
app.storage.base-dir=E:/PhotoWatermarkData
```

## Next steps

- 鎵╁睍鍚庣锛氬疄鐜版按鍗版覆鏌撱€佸浘鐗囧鍏ユ壒澶勭悊銆佸鍑洪槦鍒椾笌杩涘害 API銆?- 鎵╁睍鍓嶇锛氭帴鍏ュ浘鐗囧垪琛ㄣ€侀瑙堢敾甯冦€佹嫋鎷藉畾浣嶅拰瀵煎嚭娴佺▼ UI銆?- 鎵撳寘涓庡彂甯冿細浣跨敤 `jpackage` 鐢熸垚鑷甫杩愯鏃剁殑 Windows 瀹夎鍖?缁胯壊鐗堬紝骞跺湪 GitHub Release 鎻愪緵涓嬭浇銆