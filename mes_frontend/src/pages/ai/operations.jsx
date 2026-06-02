import ModulePage from 'pages/common/ModulePage';

export default function AiOperations() {
  return (
    <ModulePage
      title="AI 운영 분석"
      description="운영 조회 챗봇과 이벤트 알림을 넘어, 현재 흐름을 주기적으로 해석해 병목, 이상 징후, 인수인계 요약 후보를 제공하는 화면입니다."
      status="기획 반영"
      metrics={[
        { label: '분석 카드', value: '-' },
        { label: '이상 후보', value: '-' },
        { label: '알림 후보', value: '-' },
        { label: '리포트', value: '-' }
      ]}
      tasks={[
        { title: '현황 요약', description: 'MES/MCS 주요 지표를 사람이 읽기 쉬운 운영 문장으로 변환합니다.' },
        { title: '이상 징후 탐지', description: '재고 부족, 이동 지연, 반복 오류 같은 패턴을 찾아냅니다.' },
        { title: '이벤트 알림 판단', description: '인터락/오류 이벤트 중 실제 알림이 필요한 건을 선별합니다.' },
        { title: '교대 리포트', description: '조회형 챗봇과 별개로 정해진 시간에 인수인계 초안을 생성합니다.' }
      ]}
    />
  );
}
