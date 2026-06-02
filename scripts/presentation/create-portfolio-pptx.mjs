import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const root = process.cwd();
const outDir = path.join(root, 'docs', 'presentation');
const workDir = path.join(root, 'tmp', 'mes-mcs-pptx');
const output = path.join(outDir, 'mes-mcs-ai-portfolio.pptx');

fs.rmSync(workDir, { recursive: true, force: true });
fs.mkdirSync(workDir, { recursive: true });
fs.mkdirSync(outDir, { recursive: true });

const W = 13.333;
const H = 7.5;
const EMU = 914400;
const C = {
  navy: '0B1324',
  ink: '102033',
  blue: '1E63F3',
  cyan: '17B6C7',
  mint: '20C997',
  amber: 'F5B542',
  red: 'F05252',
  white: 'FFFFFF',
  paper: 'F6F8FB',
  cloud: 'EAF1F7',
  line: 'D7E1EA',
  muted: '6B7A8C',
  slate: '27364A',
};

const slides = [
  {
    kind: 'title',
    title: 'MES/MCS 통합 제조 운영 시스템',
    subtitle: '생산 실행, 자재 이동, PLC 이벤트, AI 운영 분석을 하나의 시연 흐름으로 연결한 포트폴리오 프로젝트',
    chips: ['Spring Boot', 'React', 'MariaDB', 'Spring AI', 'PLC Simulator'],
  },
  {
    title: '프로젝트가 해결하는 문제',
    subtitle: '생산 시스템과 자재 이동 시스템이 분리되어 있을 때 생기는 운영 공백을 줄입니다.',
    cards: [
      ['MES', '무엇을 생산할지, 작업오더와 생산 상태를 관리합니다.'],
      ['MCS', '어떤 자재를 어느 Location에서 어디로 이동할지 실행합니다.'],
      ['PLC', '실제 설비 이벤트를 대신해 이동 성공, 실패, 인터락을 재현합니다.'],
      ['AI', '운영 데이터를 읽고 문제 원인과 다음 조치를 설명합니다.'],
    ],
  },
  {
    title: '전체 시스템 구조',
    subtitle: '백엔드는 MES/MCS를 분리하고, React 화면은 운영자가 한 곳에서 상태를 확인하도록 구성했습니다.',
    architecture: true,
  },
  {
    title: 'MES와 MCS의 역할 분리',
    subtitle: 'MES는 생산 요청을 만들고, MCS는 자재 이동의 실행 세부사항을 결정합니다.',
    split: [
      ['MES', ['작업오더 생성', '생산계획/품목/설비 관리', '자재 요청', '작업 시작/완료 판단']],
      ['MCS', ['재고 LOT 선택', '출발/도착 Location 관리', '경로 계산', 'PLC 이벤트 기반 이동 상태 처리']],
    ],
  },
  {
    title: '핵심 업무 흐름',
    subtitle: '작업오더가 실제로 시작되기 전 자재 이동 완료 여부를 시스템적으로 확인합니다.',
    flow: ['작업오더 생성', '자재 요청', 'MCS 이동오더 생성', '경로 계산', 'PLC 이벤트', '작업 시작 허용'],
  },
  {
    title: 'MCS 경로 최적화',
    subtitle: 'Location과 이동 구간을 DB에 저장하고 그래프처럼 계산해 최적 이동 경로를 산출합니다.',
    route: true,
  },
  {
    title: 'PLC 이벤트와 인터락',
    subtitle: '성공뿐 아니라 설비 오류, 센서 불일치, 인터락 차단 같은 실패 시나리오를 재현합니다.',
    events: [
      ['TRANSFER_STARTED', '이동 시작', C.blue],
      ['TRANSFER_COMPLETED', '이동 완료', C.mint],
      ['EQUIPMENT_ERROR', '설비 오류', C.red],
      ['INTERLOCK_BLOCKED', '인터락 차단', C.amber],
    ],
  },
  {
    title: 'AI 운영 기능',
    subtitle: 'AI는 상태 변경 주체가 아니라 운영자가 이해할 수 있도록 분석하고 설명하는 보조 역할입니다.',
    ai: [
      ['AI 운영 분석', '현재 리스크, 지연, 실패 원인을 요약합니다.'],
      ['자연어 운영 조회', '작업오더나 이동오더의 상태를 질문 형태로 조회합니다.'],
      ['AI 이벤트 알림', '오류 발생 시 원인, 영향, 조치 방향을 문장으로 만듭니다.'],
    ],
  },
  {
    title: 'AI 분석 데이터 구성',
    subtitle: 'AI에게 DB 전체를 맡기지 않고, 백엔드가 근거 데이터를 모아 컨텍스트로 전달합니다.',
    context: true,
  },
  {
    title: '시연 시나리오 3가지',
    subtitle: '정상 흐름뿐 아니라 실패와 복구까지 보여줘 운영 시스템다운 완성도를 강조합니다.',
    scenarios: [
      ['정상 이동', 'MCS 이동 완료 후 MES 작업 시작 가능'],
      ['실패 이동', 'PLC 오류 발생 시 MES 작업 시작 차단'],
      ['실패 후 복구', '실패 건 취소 후 새 이동 요청으로 복구'],
    ],
  },
  {
    title: '기술 스택과 구현 포인트',
    subtitle: '단순 CRUD가 아니라 시스템 간 책임 분리, 상태 전이, 운영 이벤트 처리를 중심으로 구현했습니다.',
    tech: [
      ['Backend', 'Java 21, Spring Boot 3.3.5, MyBatis'],
      ['Frontend', 'React, Vite, MUI/Mantis 스타일'],
      ['Database', 'MariaDB, MES_DB 통합, MCS_ 접두어'],
      ['AI/PLC', 'Spring AI, OpenAI API, PowerShell Simulator'],
    ],
  },
  {
    title: '다음 확장 계획',
    subtitle: 'AI 기능을 운영 분석에서 자연어 조회와 알림까지 단계적으로 확장합니다.',
    roadmap: [
      ['1', 'AI 운영 스냅샷/분석 API'],
      ['2', 'Spring AI 모델 연동'],
      ['3', 'React AI 분석 패널'],
      ['4', '자연어 조회와 이벤트 알림'],
    ],
  },
];

function esc(v) {
  return String(v)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function emu(v) {
  return Math.round(v * EMU);
}

function ensure(file) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
}

function write(file, data) {
  const full = path.join(workDir, file);
  ensure(full);
  fs.writeFileSync(full, data, 'utf8');
}

function solid(color) {
  return `<a:solidFill><a:srgbClr val="${color}"/></a:solidFill>`;
}

function textRuns(lines, size = 2400, color = C.ink, bold = false) {
  return lines.map((line, idx) => `
    <a:p>
      <a:pPr marL="0" indent="0"/>
      <a:r>
        <a:rPr lang="ko-KR" sz="${size}"${bold ? ' b="1"' : ''}>
          <a:solidFill><a:srgbClr val="${color}"/></a:solidFill>
          <a:latin typeface="Malgun Gothic"/>
          <a:ea typeface="Malgun Gothic"/>
        </a:rPr>
        <a:t>${esc(line)}</a:t>
      </a:r>
      ${idx === lines.length - 1 ? '<a:endParaRPr lang="ko-KR"/>' : ''}
    </a:p>`).join('');
}

let shapeId = 10;
function box(x, y, w, h, fill, line = 'transparent', radius = false) {
  const ln = line === 'transparent' ? '<a:ln><a:noFill/></a:ln>' : `<a:ln w="12000">${solid(line)}</a:ln>`;
  return `
  <p:sp>
    <p:nvSpPr><p:cNvPr id="${shapeId++}" name="Box"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
    <p:spPr>
      <a:xfrm><a:off x="${emu(x)}" y="${emu(y)}"/><a:ext cx="${emu(w)}" cy="${emu(h)}"/></a:xfrm>
      <a:prstGeom prst="${radius ? 'roundRect' : 'rect'}"><a:avLst/></a:prstGeom>
      ${solid(fill)}
      ${ln}
    </p:spPr>
    <p:txBody><a:bodyPr/><a:lstStyle/><a:p/></p:txBody>
  </p:sp>`;
}

function text(x, y, w, h, lines, opts = {}) {
  const { size = 2400, color = C.ink, bold = false, fill = null, align = 'l' } = opts;
  const bodyPr = `<a:bodyPr wrap="square" lIns="91440" tIns="45720" rIns="91440" bIns="45720"/>`;
  const fillXml = fill ? `${solid(fill)}` : '<a:noFill/>';
  const paragraphs = lines.map((line) => `
    <a:p>
      <a:pPr algn="${align}"/>
      <a:r>
        <a:rPr lang="ko-KR" sz="${size}"${bold ? ' b="1"' : ''}>
          <a:solidFill><a:srgbClr val="${color}"/></a:solidFill>
          <a:latin typeface="Malgun Gothic"/>
          <a:ea typeface="Malgun Gothic"/>
        </a:rPr>
        <a:t>${esc(line)}</a:t>
      </a:r>
      <a:endParaRPr lang="ko-KR"/>
    </a:p>`).join('');
  return `
  <p:sp>
    <p:nvSpPr><p:cNvPr id="${shapeId++}" name="Text"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
    <p:spPr>
      <a:xfrm><a:off x="${emu(x)}" y="${emu(y)}"/><a:ext cx="${emu(w)}" cy="${emu(h)}"/></a:xfrm>
      <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
      ${fillXml}<a:ln><a:noFill/></a:ln>
    </p:spPr>
    <p:txBody>${bodyPr}<a:lstStyle/>${paragraphs}</p:txBody>
  </p:sp>`;
}

function pill(x, y, label, fill, color = C.white) {
  return box(x, y, 1.45, 0.34, fill, 'transparent', true) +
    text(x + 0.05, y + 0.03, 1.35, 0.25, [label], { size: 1200, color, bold: true, align: 'c' });
}

function connector(x1, y1, x2, y2, color = C.line, width = 22000) {
  return `
  <p:cxnSp>
    <p:nvCxnSpPr><p:cNvPr id="${shapeId++}" name="Connector"/><p:cNvCxnSpPr/><p:nvPr/></p:nvCxnSpPr>
    <p:spPr>
      <a:xfrm><a:off x="${emu(Math.min(x1, x2))}" y="${emu(Math.min(y1, y2))}"/><a:ext cx="${emu(Math.abs(x2 - x1))}" cy="${emu(Math.abs(y2 - y1))}"/></a:xfrm>
      <a:prstGeom prst="line"><a:avLst/></a:prstGeom>
      <a:ln w="${width}">${solid(color)}</a:ln>
    </p:spPr>
  </p:cxnSp>`;
}

function header(s) {
  return text(0.62, 0.34, 8.8, 0.48, [s.title], { size: 2450, color: C.navy, bold: true }) +
    text(0.62, 0.86, 10.7, 0.42, [s.subtitle], { size: 1050, color: C.muted }) +
    box(0.62, 1.35, 11.95, 0.02, C.line);
}

function accentBackground(dark = false) {
  const bg = dark ? C.navy : C.paper;
  return box(0, 0, W, H, bg) +
    box(10.55, -0.35, 3.1, 8.2, dark ? '14213A' : 'E9F7F6') +
    box(10.82, 0.55, 0.09, 5.9, dark ? C.mint : C.cyan) +
    box(0, 7.1, W, 0.4, dark ? '101A30' : 'EAF1F7');
}

function renderSlide(s, idx) {
  shapeId = 10;
  let xml = '';
  if (s.kind === 'title') {
    xml += accentBackground(true);
    xml += text(0.72, 0.72, 2.2, 0.34, ['PORTFOLIO'], { size: 1150, color: C.mint, bold: true });
    xml += text(0.7, 1.32, 8.6, 1.25, [s.title], { size: 3200, color: C.white, bold: true });
    xml += text(0.76, 2.58, 7.5, 0.85, [s.subtitle], { size: 1350, color: 'C8D7E5' });
    s.chips.forEach((chip, i) => { xml += pill(0.78 + i * 1.55, 4.36, chip, i % 2 ? C.cyan : C.blue); });
    xml += box(8.85, 1.15, 3.15, 3.85, '152640', '274B6F', true);
    xml += text(9.15, 1.58, 2.5, 0.5, ['MES'], { size: 2300, color: C.white, bold: true, align: 'c' });
    xml += text(9.15, 2.5, 2.5, 0.5, ['MCS'], { size: 2300, color: C.white, bold: true, align: 'c' });
    xml += text(9.15, 3.42, 2.5, 0.5, ['PLC'], { size: 2300, color: C.white, bold: true, align: 'c' });
    xml += text(9.15, 4.34, 2.5, 0.5, ['AI'], { size: 2300, color: C.white, bold: true, align: 'c' });
    xml += connector(10.42, 2.1, 10.42, 4.15, C.mint, 28000);
  } else {
    xml += accentBackground(false) + header(s);
    if (s.cards) {
      s.cards.forEach((c, i) => {
        const x = 0.78 + (i % 2) * 5.7;
        const y = 1.78 + Math.floor(i / 2) * 2.0;
        xml += box(x, y, 5.05, 1.45, C.white, C.line, true);
        xml += box(x, y, 0.18, 1.45, [C.blue, C.mint, C.amber, C.cyan][i]);
        xml += text(x + 0.38, y + 0.22, 1.4, 0.35, [c[0]], { size: 1650, color: C.navy, bold: true });
        xml += text(x + 0.38, y + 0.72, 4.18, 0.45, [c[1]], { size: 1050, color: C.slate });
      });
    }
    if (s.architecture) {
      const nodes = [
        ['React Frontend', 0.95, 2.35, C.blue],
        ['MES Backend', 3.7, 1.9, C.mint],
        ['MCS Backend', 3.7, 3.2, C.cyan],
        ['MES_DB', 6.85, 2.55, C.amber],
        ['PLC Simulator', 9.45, 3.2, C.red],
        ['Spring AI', 9.45, 1.9, C.blue],
      ];
      xml += connector(2.75, 2.8, 3.7, 2.18, C.line);
      xml += connector(2.75, 2.8, 3.7, 3.48, C.line);
      xml += connector(6.0, 2.25, 6.85, 2.8, C.line);
      xml += connector(6.0, 3.55, 6.85, 2.9, C.line);
      xml += connector(8.6, 2.8, 9.45, 2.18, C.line);
      xml += connector(8.6, 2.9, 9.45, 3.48, C.line);
      nodes.forEach(([label, x, y, color]) => {
        xml += box(x, y, 2.05, 0.82, C.white, color, true);
        xml += text(x + 0.08, y + 0.23, 1.88, 0.3, [label], { size: 1150, color: C.navy, bold: true, align: 'c' });
      });
      xml += text(0.9, 5.55, 9.8, 0.55, ['백엔드는 분리하고, DB는 통합해 MES/MCS 연동 시나리오를 단순하면서도 명확하게 구성했습니다.'], { size: 1200, color: C.slate });
    }
    if (s.split) {
      s.split.forEach((col, i) => {
        const x = i === 0 ? 0.85 : 6.55;
        xml += box(x, 1.75, 5.1, 4.15, C.white, i === 0 ? C.blue : C.mint, true);
        xml += text(x + 0.28, 2.03, 2.3, 0.42, [col[0]], { size: 1800, color: i === 0 ? C.blue : C.mint, bold: true });
        col[1].forEach((item, j) => {
          xml += box(x + 0.35, 2.76 + j * 0.72, 0.22, 0.22, i === 0 ? C.blue : C.mint, 'transparent', true);
          xml += text(x + 0.68, 2.66 + j * 0.72, 3.9, 0.36, [item], { size: 1120, color: C.slate });
        });
      });
    }
    if (s.flow) {
      s.flow.forEach((label, i) => {
        const x = 0.55 + i * 2.02;
        xml += box(x, 2.45, 1.55, 1.02, C.white, i % 2 ? C.mint : C.blue, true);
        xml += text(x + 0.1, 2.78, 1.35, 0.3, [label], { size: 920, color: C.navy, bold: true, align: 'c' });
        if (i < s.flow.length - 1) xml += connector(x + 1.55, 2.96, x + 2.02, 2.96, C.amber, 24000);
      });
      xml += box(1.0, 4.65, 10.8, 0.88, 'EEF8F5', C.mint, true);
      xml += text(1.22, 4.9, 10.2, 0.3, ['MCS 이동이 완료되어야 MES 작업 시작이 가능하도록 인터락을 적용했습니다.'], { size: 1250, color: C.navy, bold: true, align: 'c' });
    }
    if (s.route) {
      const pts = [
        ['NCM-01-01', 1.0, 2.55, C.blue],
        ['CV-01', 4.3, 2.55, C.amber],
        ['BUF-01', 4.3, 4.2, C.cyan],
        ['NCM-01-02', 7.75, 2.55, C.mint],
      ];
      xml += connector(2.5, 2.96, 4.3, 2.96, C.red, 26000);
      xml += connector(5.45, 2.96, 7.75, 2.96, C.line, 22000);
      xml += connector(1.8, 3.45, 4.3, 4.6, C.mint, 22000);
      xml += connector(5.45, 4.6, 8.4, 3.45, C.mint, 22000);
      pts.forEach(([label, x, y, color]) => {
        xml += box(x, y, 1.55, 0.9, C.white, color, true);
        xml += text(x + 0.07, y + 0.28, 1.42, 0.25, [label], { size: 1000, color: C.navy, bold: true, align: 'c' });
      });
      xml += pill(1.04, 5.64, '막힘 구간 제외', C.red);
      xml += pill(2.65, 5.64, '혼잡 회피', C.amber, C.navy);
      xml += pill(4.27, 5.64, '최단 시간', C.blue);
    }
    if (s.events) {
      s.events.forEach((e, i) => {
        const x = 0.95 + (i % 2) * 5.35;
        const y = 1.8 + Math.floor(i / 2) * 1.55;
        xml += box(x, y, 4.75, 1.05, C.white, e[2], true);
        xml += text(x + 0.28, y + 0.22, 2.7, 0.28, [e[0]], { size: 1120, color: e[2], bold: true });
        xml += text(x + 0.28, y + 0.58, 3.6, 0.28, [e[1]], { size: 1200, color: C.navy, bold: true });
      });
      xml += box(0.95, 5.35, 10.15, 0.72, 'FFF8E7', C.amber, true);
      xml += text(1.16, 5.56, 9.7, 0.25, ['실패 이벤트는 이력으로 남고, MES 작업 시작 차단과 복구 시나리오의 근거가 됩니다.'], { size: 1100, color: C.navy, align: 'c' });
    }
    if (s.ai) {
      s.ai.forEach((a, i) => {
        const x = 0.9 + i * 3.65;
        xml += box(x, 2.0, 3.1, 2.8, C.white, [C.blue, C.mint, C.amber][i], true);
        xml += text(x + 0.24, 2.32, 2.5, 0.38, [a[0]], { size: 1380, color: [C.blue, C.mint, C.amber][i], bold: true });
        xml += text(x + 0.24, 3.05, 2.45, 0.8, [a[1]], { size: 1050, color: C.slate });
      });
      xml += text(1.12, 5.35, 10.4, 0.45, ['AI는 “판단 근거 없는 자동 제어”가 아니라, 운영자가 빠르게 이해하도록 돕는 설명 계층입니다.'], { size: 1150, color: C.navy, bold: true, align: 'c' });
    }
    if (s.context) {
      const stages = [
        ['기존 Service', '작업오더, 이동오더, 재고, PLC 이벤트 조회'],
        ['Context DTO', '사실 데이터와 차단 사유를 구조화'],
        ['LLM', '원인 후보와 조치 방향을 자연어로 설명'],
      ];
      stages.forEach((st, i) => {
        const x = 0.9 + i * 3.75;
        xml += box(x, 2.2, 3.05, 1.45, C.white, [C.blue, C.cyan, C.mint][i], true);
        xml += text(x + 0.22, 2.48, 2.5, 0.3, [st[0]], { size: 1350, color: C.navy, bold: true, align: 'c' });
        xml += text(x + 0.22, 2.9, 2.55, 0.42, [st[1]], { size: 900, color: C.slate, align: 'c' });
        if (i < 2) xml += connector(x + 3.05, 2.92, x + 3.75, 2.92, C.amber, 24000);
      });
      xml += box(1.1, 4.82, 9.7, 0.82, 'EEF8FF', C.blue, true);
      xml += text(1.36, 5.05, 9.1, 0.3, ['추론 품질은 “AI에게 어떤 근거 데이터를 주는가”에 따라 결정됩니다.'], { size: 1200, color: C.navy, bold: true, align: 'c' });
    }
    if (s.scenarios) {
      s.scenarios.forEach((sc, i) => {
        const y = 1.88 + i * 1.32;
        xml += box(0.95, y, 10.5, 0.88, C.white, [C.mint, C.red, C.blue][i], true);
        xml += box(1.22, y + 0.22, 0.44, 0.44, [C.mint, C.red, C.blue][i], 'transparent', true);
        xml += text(1.33, y + 0.26, 0.22, 0.2, [String(i + 1)], { size: 900, color: C.white, bold: true, align: 'c' });
        xml += text(1.88, y + 0.23, 2.3, 0.28, [sc[0]], { size: 1250, color: C.navy, bold: true });
        xml += text(4.15, y + 0.23, 6.5, 0.28, [sc[1]], { size: 1100, color: C.slate });
      });
    }
    if (s.tech) {
      s.tech.forEach((t, i) => {
        const x = 0.8 + (i % 2) * 5.6;
        const y = 1.78 + Math.floor(i / 2) * 1.65;
        xml += box(x, y, 5.0, 1.15, C.white, [C.blue, C.mint, C.amber, C.cyan][i], true);
        xml += text(x + 0.24, y + 0.23, 1.55, 0.28, [t[0]], { size: 1200, color: [C.blue, C.mint, C.amber, C.cyan][i], bold: true });
        xml += text(x + 1.85, y + 0.23, 2.9, 0.42, [t[1]], { size: 1000, color: C.slate });
      });
    }
    if (s.roadmap) {
      s.roadmap.forEach((r, i) => {
        const x = 1.0 + i * 2.65;
        xml += box(x, 2.25, 2.05, 2.35, C.white, [C.blue, C.cyan, C.mint, C.amber][i], true);
        xml += box(x + 0.7, 2.55, 0.65, 0.65, [C.blue, C.cyan, C.mint, C.amber][i], 'transparent', true);
        xml += text(x + 0.72, 2.71, 0.6, 0.2, [r[0]], { size: 1150, color: C.white, bold: true, align: 'c' });
        xml += text(x + 0.25, 3.45, 1.55, 0.45, [r[1]], { size: 1050, color: C.navy, bold: true, align: 'c' });
      });
      xml += text(1.2, 5.45, 9.8, 0.45, ['1차는 gpt-5.4-mini로 비용을 낮추고, 복잡한 장애 분석만 상위 모델로 라우팅합니다.'], { size: 1100, color: C.slate, align: 'c' });
    }
  }

  xml += text(11.5, 7.02, 1.0, 0.25, [`${idx + 1}/${slides.length}`], { size: 820, color: '7B8B9D', align: 'r' });
  return slideXml(xml);
}

function slideXml(content) {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
       xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld><p:spTree>
    <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
    <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
    ${content}
  </p:spTree></p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>`;
}

function emptyRels() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>`;
}

write('[Content_Types].xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  <Override PartName="/ppt/presProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presProps+xml"/>
  <Override PartName="/ppt/viewProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.viewProps+xml"/>
  ${slides.map((_, i) => `<Override PartName="/ppt/slides/slide${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>`).join('\n  ')}
</Types>`);

write('_rels/.rels', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>`);

write('ppt/presentation.xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst>
  <p:sldIdLst>
    ${slides.map((_, i) => `<p:sldId id="${256 + i}" r:id="rId${i + 2}"/>`).join('\n    ')}
  </p:sldIdLst>
  <p:sldSz cx="${emu(W)}" cy="${emu(H)}" type="wide"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>`);

write('ppt/_rels/presentation.xml.rels', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
  ${slides.map((_, i) => `<Relationship Id="rId${i + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide${i + 1}.xml"/>`).join('\n  ')}
</Relationships>`);

write('ppt/slideMasters/slideMaster1.xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
  <p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles>
</p:sldMaster>`);
write('ppt/slideMasters/_rels/slideMaster1.xml.rels', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>`);
write('ppt/slideLayouts/slideLayout1.xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank">
  <p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>`);
write('ppt/slideLayouts/_rels/slideLayout1.xml.rels', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>`);
write('ppt/theme/theme1.xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="MES MCS Theme">
  <a:themeElements>
    <a:clrScheme name="MESMCS"><a:dk1><a:srgbClr val="${C.navy}"/></a:dk1><a:lt1><a:srgbClr val="${C.white}"/></a:lt1><a:dk2><a:srgbClr val="${C.ink}"/></a:dk2><a:lt2><a:srgbClr val="${C.paper}"/></a:lt2><a:accent1><a:srgbClr val="${C.blue}"/></a:accent1><a:accent2><a:srgbClr val="${C.mint}"/></a:accent2><a:accent3><a:srgbClr val="${C.amber}"/></a:accent3><a:accent4><a:srgbClr val="${C.cyan}"/></a:accent4><a:accent5><a:srgbClr val="${C.red}"/></a:accent5><a:accent6><a:srgbClr val="${C.slate}"/></a:accent6><a:hlink><a:srgbClr val="${C.blue}"/></a:hlink><a:folHlink><a:srgbClr val="${C.cyan}"/></a:folHlink></a:clrScheme>
    <a:fontScheme name="MESMCS"><a:majorFont><a:latin typeface="Malgun Gothic"/><a:ea typeface="Malgun Gothic"/></a:majorFont><a:minorFont><a:latin typeface="Malgun Gothic"/><a:ea typeface="Malgun Gothic"/></a:minorFont></a:fontScheme>
    <a:fmtScheme name="MESMCS"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="6350"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme>
  </a:themeElements>
</a:theme>`);
write('ppt/presProps.xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:presentationPr xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"/>`);
write('ppt/viewProps.xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:viewPr xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"/>`);

slides.forEach((slide, i) => {
  write(`ppt/slides/slide${i + 1}.xml`, renderSlide(slide, i));
  write(`ppt/slides/_rels/slide${i + 1}.xml.rels`, emptyRels());
});

fs.rmSync(output, { force: true });
const zipOutput = output.replace(/\.pptx$/i, '.zip');
fs.rmSync(zipOutput, { force: true });
execFileSync('powershell.exe', [
  '-NoProfile',
  '-Command',
  `Compress-Archive -Path '${workDir}\\*' -DestinationPath '${zipOutput}' -Force`
], { stdio: 'inherit' });
fs.renameSync(zipOutput, output);

console.log(output);
