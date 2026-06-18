import { mesApi } from './client';

// PLC 이벤트 시뮬레이터 (데모/테스트용) — 백엔드 dev 전용 엔드포인트
export const plcSimApi = {
  scenarios: () => mesApi.get('/api/v1/dev/plc-sim/scenarios'),
  fire: (transferId, scenario) => mesApi.post('/api/v1/dev/plc-sim', { transferId, scenario })
};
