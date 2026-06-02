# React MES/MCS 실행 점검

## 목적

React 전환 후 MES(8080), MCS(8081), React(3000)을 같이 실행하고 API 연결을 확인한다.

## 주의

DB 비밀번호는 Git에 저장하지 않는다. `application-local.yml`은 `${MES_DB_PASSWORD}` 형식을 사용하므로 실행 시 환경변수나 실행 스크립트 입력값으로만 전달한다.

## 실행

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\dev\start-backends.ps1
```

프롬프트에 `MES_DB_PASSWORD`를 입력하면 MES/MCS 백엔드가 현재 소스로 재기동된다.

React 서버가 꺼져 있으면:

```powershell
cd .\mes_frontend
corepack yarn dev --host 127.0.0.1 --port 3000
```

## 확인 URL

- React: `http://127.0.0.1:3000`
- MES ping: `http://127.0.0.1:8080/api/v1/ping`
- MES plants: `http://127.0.0.1:8080/api/v1/master/plants?useYn=Y`
- MCS plants: `http://127.0.0.1:8081/api/references/plants`
- MCS transfers: `http://127.0.0.1:8081/api/transfers?page=1&size=5`

## 시나리오 점검 순서

1. MES 기준정보 조회
2. MES 생산계획 등록
3. MES 작업오더 등록 및 시작/완료
4. MES 생산실적 등록
5. MES 재고 입출고 등록
6. MES 품질검사/불량이력 등록
7. MCS Location/재고 조회
8. MCS 이동오더 등록, 품목 추가, 시작/완료
9. PLC 시뮬레이터 실행 후 PLC 이벤트 로그 확인

## 현재 발견된 실행 이슈

`MES_DB_PASSWORD`가 없는 상태로 백엔드를 실행하면 Aiven DB 접속이 `Access denied for user 'avnadmin'` 오류로 실패한다. 이 경우 코드는 빌드되지만 API 조회가 500이 되거나 서버가 기동 실패할 수 있다.
