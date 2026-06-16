import { useMemo, useState } from 'react';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import LinearProgress from '@mui/material/LinearProgress';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { DeleteOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { aiApi } from 'api/mes/ai';

const DOCUMENT_CATEGORIES = [
  { value: 'PLC_EQUIPMENT', label: 'PLC/설비' },
  { value: 'QUALITY', label: '품질' },
  { value: 'PRODUCTION', label: '생산' },
  { value: 'INVENTORY_LOGISTICS', label: '재고/물류' },
  { value: 'SAFETY', label: '안전' },
  { value: 'ETC', label: '기타' }
];

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString('ko-KR');
}

function statusColor(status) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'FAILED') return 'error';
  if (status === 'PROCESSING') return 'warning';
  return 'default';
}

function categoryLabel(value) {
  return DOCUMENT_CATEGORIES.find((item) => item.value === value)?.label || value || '-';
}

export default function AiRagDocuments() {
  const [documentCategory, setDocumentCategory] = useState(DOCUMENT_CATEGORIES[0].value);
  const [documentType, setDocumentType] = useState('');
  const [tags, setTags] = useState('');
  const [file, setFile] = useState(null);
  const [working, setWorking] = useState(false);
  const [message, setMessage] = useState(null);

  const { data, error, isLoading, mutate } = useSWR(['ai-rag-documents'], () => aiApi.getRagDocuments(), {
    revalidateOnFocus: false
  });

  const documents = useMemo(() => getApiData(data, []), [data]);

  const handleUpload = async () => {
    if (!file) {
      setMessage({ severity: 'warning', text: '업로드할 파일을 선택해 주세요.' });
      return;
    }
    if (!documentType.trim()) {
      setMessage({ severity: 'warning', text: '상세 문서 유형을 입력해 주세요.' });
      return;
    }

    setWorking(true);
    setMessage(null);
    try {
      await aiApi.uploadRagDocument({ file, documentCategory, documentType: documentType.trim(), tags: tags.trim() });
      setFile(null);
      setDocumentType('');
      setTags('');
      setMessage({ severity: 'success', text: '문서를 업로드하고 RAG 검색용으로 등록했습니다.' });
      await mutate();
    } catch (uploadError) {
      setMessage({ severity: 'error', text: uploadError.message || '문서 업로드에 실패했습니다.' });
    } finally {
      setWorking(false);
    }
  };

  const handleReindex = async (documentId) => {
    setWorking(true);
    setMessage(null);
    try {
      await aiApi.reindexRagDocument(documentId);
      setMessage({ severity: 'success', text: '문서를 다시 색인했습니다.' });
      await mutate();
    } catch (reindexError) {
      setMessage({ severity: 'error', text: reindexError.message || '재색인에 실패했습니다.' });
    } finally {
      setWorking(false);
    }
  };

  const handleReindexAll = async () => {
    setWorking(true);
    setMessage(null);
    try {
      const response = await aiApi.reindexAllRagDocuments();
      const results = getApiData(response, []);
      setMessage({ severity: 'success', text: `등록된 문서 ${results.length}건을 다시 색인했습니다.` });
      await mutate();
    } catch (reindexError) {
      setMessage({ severity: 'error', text: reindexError.message || '전체 재색인에 실패했습니다.' });
    } finally {
      setWorking(false);
    }
  };

  const handleDelete = async (documentId) => {
    setWorking(true);
    setMessage(null);
    try {
      await aiApi.deleteRagDocument(documentId);
      setMessage({ severity: 'success', text: '문서를 삭제했습니다.' });
      await mutate();
    } catch (deleteError) {
      setMessage({ severity: 'error', text: deleteError.message || '삭제에 실패했습니다.' });
    } finally {
      setWorking(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h3">RAG 문서 관리</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
          운영 문서를 업로드하면 백엔드가 텍스트를 추출하고 Chroma 벡터 DB에 등록합니다.
        </Typography>
      </Box>

      <MainCard title="문서 업로드">
        <Stack spacing={2}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} sx={{ alignItems: { md: 'center' } }}>
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel id="rag-document-category-label">문서 분류</InputLabel>
              <Select
                labelId="rag-document-category-label"
                label="문서 분류"
                value={documentCategory}
                onChange={(event) => setDocumentCategory(event.target.value)}
                disabled={working}
              >
                {DOCUMENT_CATEGORIES.map((category) => (
                  <MenuItem key={category.value} value={category.value}>
                    {category.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              size="small"
              label="상세 문서 유형"
              placeholder="예: CV-001 PLC 코드 설명서"
              value={documentType}
              onChange={(event) => setDocumentType(event.target.value)}
              disabled={working}
              sx={{ minWidth: 240 }}
            />

            <TextField
              size="small"
              label="태그"
              placeholder="예: PLC, CV-001, TO_LOCATION_CD"
              value={tags}
              onChange={(event) => setTags(event.target.value)}
              disabled={working}
              sx={{ minWidth: 260 }}
            />

            <Button component="label" variant="outlined" startIcon={<UploadOutlined />} disabled={working}>
              파일 선택
              <input
                type="file"
                hidden
                accept=".md,.txt,.pdf,.docx,.xlsx,.scl,.st,.l5x,.awl"
                onChange={(event) => setFile(event.target.files?.[0] || null)}
              />
            </Button>

            <Typography variant="body2" color="text.secondary" sx={{ flex: 1, minWidth: 0 }}>
              {file ? file.name : 'md, txt, pdf, docx, xlsx, scl, st, l5x, awl 파일을 업로드할 수 있습니다.'}
            </Typography>

            <Button variant="contained" onClick={handleUpload} disabled={working || !file || !documentType.trim()}>
              등록
            </Button>
          </Stack>

          {message && <Alert severity={message.severity}>{message.text}</Alert>}
        </Stack>
      </MainCard>

      <MainCard
        title="등록된 문서"
        secondary={
          <Button
            size="small"
            variant="outlined"
            startIcon={<ReloadOutlined />}
            disabled={working || documents.length === 0}
            onClick={handleReindexAll}
          >
            전체 재색인
          </Button>
        }
      >
        {(isLoading || working) && <LinearProgress sx={{ mb: 2 }} />}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            문서 목록을 불러오지 못했습니다.
          </Alert>
        )}

        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>파일명</TableCell>
                <TableCell>분류</TableCell>
                <TableCell>상세 유형</TableCell>
                <TableCell>태그</TableCell>
                <TableCell>상태</TableCell>
                <TableCell align="right">청크</TableCell>
                <TableCell>등록일</TableCell>
                <TableCell align="right">작업</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {documents.length === 0 && (
                <TableRow>
                  <TableCell colSpan={8}>
                    <Typography variant="body2" color="text.secondary" align="center" sx={{ py: 3 }}>
                      등록된 RAG 문서가 없습니다.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
              {documents.map((document) => (
                <TableRow key={document.documentId} hover>
                  <TableCell>
                    <Stack spacing={0.25}>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {document.originalFileName}
                      </Typography>
                      {document.errorMessage && (
                        <Typography variant="caption" color="error">
                          {document.errorMessage}
                        </Typography>
                      )}
                    </Stack>
                  </TableCell>
                  <TableCell>{categoryLabel(document.documentCategory)}</TableCell>
                  <TableCell>{document.documentType}</TableCell>
                  <TableCell>
                    <Typography variant="caption" color="text.secondary">
                      {document.tags || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip label={document.status} color={statusColor(document.status)} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell align="right">{document.chunkCount ?? 0}</TableCell>
                  <TableCell>{formatDate(document.createdAt)}</TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={0.75} sx={{ justifyContent: 'flex-end' }}>
                      <Button
                        size="small"
                        variant="outlined"
                        startIcon={<ReloadOutlined />}
                        disabled={working}
                        onClick={() => handleReindex(document.documentId)}
                      >
                        재색인
                      </Button>
                      <Button
                        size="small"
                        color="error"
                        variant="outlined"
                        startIcon={<DeleteOutlined />}
                        disabled={working}
                        onClick={() => handleDelete(document.documentId)}
                      >
                        삭제
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </MainCard>
    </Stack>
  );
}
