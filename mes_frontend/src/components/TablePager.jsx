import Box from '@mui/material/Box';
import Pagination from '@mui/material/Pagination';

// 공용 페이징 바 — 모든 목록 페이지에서 동일한 "가운데 정렬 숫자 페이지네이션"을 쓰기 위한 컴포넌트.
// 페이지마다 페이징 마크업을 따로 만들지 않고 이 컴포넌트를 재사용한다.
//
// props
//   page     : 현재 페이지 (1부터 시작)
//   count    : 전체 페이지 수
//   onChange : (nextPage) => void   // 1부터 시작하는 다음 페이지 번호를 전달
//   sx       : 추가 스타일 (선택)
export default function TablePager({ page, count, onChange, sx }) {
  if (!count || count <= 1) {
    return null;
  }

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', py: 2, ...sx }}>
      <Pagination
        count={count}
        page={page}
        onChange={(_event, value) => onChange(value)}
        color="primary"
        shape="rounded"
        showFirstButton
        showLastButton
      />
    </Box>
  );
}
