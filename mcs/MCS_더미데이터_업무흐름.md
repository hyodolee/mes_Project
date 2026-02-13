# MCS 더미데이터 업무 흐름 설명서

> 리튬이온 배터리 제조 공정의 창고 관리 시뮬레이션
> 기간: 2025-01-15 ~ 2025-01-25
> 공장: 오창1공장(셀 생산) + 대전2공장(팩 조립)

---

## 📚 목차

1. [시스템 구조](#1-시스템-구조)
2. [업무 흐름 개요](#2-업무-흐름-개요)
3. [상세 업무 시나리오](#3-상세-업무-시나리오)
4. [DB 테이블별 데이터 설명](#4-db-테이블별-데이터-설명)
5. [재고 현황 조회](#5-재고-현황-조회)

---

## 1. 시스템 구조

### 1.1 창고 계층 구조

```
공장 (PLANT)
  └─ 창고 (WAREHOUSE) ← MES 관리 범위
      └─ 구역 (ZONE) ← MCS 추가
          └─ 로케이션 (LOCATION) ← MCS 추가
              └─ 재고 (LOCATION_STOCK) ← MCS 상세 재고
```

### 1.2 오창1공장 창고 구조 (셀 생산)

| 창고코드 | 창고명 | 구역 예시 | 로케이션 예시 |
|---------|--------|----------|--------------|
| WH001 | 양극소재창고 | Z-NCM (NCM활물질 보관구역) | NCM-01-01 (NCM811 파렛트1) |
| WH002 | 음극소재창고 | Z-GRP (음극활물질 보관구역) | GRP-01-01 (음극활물질 파렛트1) |
| WH003 | 전해액/분리막창고 | Z-ELY (전해액 보관구역, 방폭) | ELY-01-01 (전해액 탱크1) |
| WH004 | 전극재공창고 | Z-SLR (슬러리/전극시트 구역) | SLR-01-01 (슬러리/전극 적치1) |
| WH005 | 셀재공창고 | Z-JEL (젤리롤/드라이셀 구역) | JEL-01-01 (젤리롤 적치) |
| WH006 | 완성셀창고 | Z-CEL (완성셀 보관구역) | CEL-01-01 (완성셀 파렛트1) |

### 1.3 대전2공장 창고 구조 (팩 조립)

| 창고코드 | 창고명 | 구역 예시 | 로케이션 예시 |
|---------|--------|----------|--------------|
| WH008 | 셀입고창고 | Z-IN (셀입고 보관구역) | IN-01-01 (셀입고 파렛트1) |
| WH009 | 팩부품창고 | Z-BMS (BMS/커넥터 구역) | BMS-01-01 (BMS/커넥터 랙) |
| WH010 | 모듈재공창고 | Z-MOD (모듈 보관구역) | MOD-01-01 (모듈 적치1) |
| WH011 | 완성팩창고 | Z-PCK (완성팩 보관구역) | PCK-01-01 (완성팩 적치1) |

---

## 2. 업무 흐름 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                      🏭 오창1공장 (셀 생산)                        │
└─────────────────────────────────────────────────────────────────┘

[1단계] 원자재 입고 (1/18)
   ↓
   MCS_INBOUND_ORDER (입고오더)
   MCS_INBOUND_ITEM (입고품목)
   MCS_LOCATION_STOCK (로케이션재고 증가)
   MCS_LOC_TRANS_HIS (입고이력 IB_IN)

[2단계] 생산 투입 (1/20~1/22)
   ↓
   MCS_OUTBOUND_ORDER (출고오더 - 생산투입)
   MCS_OUTBOUND_ITEM (출고품목)
   MCS_LOCATION_STOCK (원자재 재고 감소)
   MCS_LOC_TRANS_HIS (출고이력 PRD_ISSUE)

[3단계] 생산 입고 (1/20~1/22)
   ↓
   MCS_LOCATION_STOCK (반제품/완제품 재고 증가)
   MCS_LOC_TRANS_HIS (생산입고이력 PRD_RECEIPT)

[4단계] 공장간 이동 (1/20~1/22)
   ↓
   MCS_OUTBOUND_ORDER (오창 출고)
   MCS_INBOUND_ORDER (대전 입고)
   MCS_TRANSFER_ORDER (이동오더)

┌─────────────────────────────────────────────────────────────────┐
│                      🏭 대전2공장 (팩 조립)                        │
└─────────────────────────────────────────────────────────────────┘

[5단계] 셀 입고 → 모듈 조립 → 팩 조립 (1/20~1/22)
   ↓
   MCS_LOCATION_STOCK (셀/부품 감소, 모듈/팩 증가)
   MCS_LOC_TRANS_HIS (생산투입/입고 이력)

[6단계] 고객 납품 (1/22)
   ↓
   MCS_OUTBOUND_ORDER (출고오더 - 고객납품)
   MCS_OUTBOUND_ITEM (출고품목)
   MCS_LOCATION_STOCK (완성팩 재고 감소)
   MCS_LOC_TRANS_HIS (출고이력 OB_OUT)
```

---

## 3. 상세 업무 시나리오

### 📦 시나리오 1: 원자재 입고 (2025-01-18)

#### 업무 흐름
1. 거래처(에코프로비엠)에서 NCM811 양극활물질 5톤 납품
2. 입고대기구역(`R1-01`)에 임시 보관
3. 품질검사 후 합격
4. 정위치(`NCM-01-01`)로 적치

#### DB 데이터

**① MCS_INBOUND_ORDER (입고오더)**
```sql
INBOUND_ID: 1
INBOUND_NO: IB-20250118-0001
INBOUND_STATUS: COMPLETED (완료)
VENDOR_CD: V001 (에코프로비엠)
WAREHOUSE_CD: WH001 (양극소재창고)
EXPECTED_DT: 2025-01-18
ACTUAL_DT: 2025-01-18 08:30:00
```

**② MCS_INBOUND_ITEM (입고품목)**
```sql
INBOUND_ID: 1
ITEM_CD: RM-NCM-001 (NCM811 양극활물질)
LOT_NO: RM-NCM-001-20250118-001
LOCATION_ID: 1 (NCM-01-01 파렛트)
EXPECTED_QTY: 5000.000 KG
ACTUAL_QTY: 5000.000 KG
ITEM_STATUS: STOCKED (적치완료)
```

**③ MCS_LOCATION_STOCK (로케이션재고)**
```sql
LOC_STOCK_ID: 1
LOCATION_ID: 1 (NCM-01-01)
ITEM_CD: RM-NCM-001
LOT_NO: RM-NCM-001-20250118-001
STOCK_QTY: 3500.000 (현재 5000 입고 → 1500 투입 후)
RESERVED_QTY: 500.000 (생산 예약)
AVAILABLE_QTY: 3000.000 (가용재고 = 3500-500)
```

**④ MCS_LOC_TRANS_HIS (재고이력)**
```sql
LOC_STOCK_ID: 1
TRANS_TYPE: IB_IN (입고적치)
TRANS_QTY: 5000.000
BEFORE_QTY: 0.000
AFTER_QTY: 5000.000
REF_TYPE: IB
REF_NO: IB-20250118-0001
TRANS_RMK: 'NCM811 양극활물질 5톤 입고 적치'
```

**⑤ MCS_TRANSFER_ORDER (로케이션 이동)**
```sql
TRANSFER_ID: 1
TRANSFER_NO: TF-20250118-0001
TRANSFER_STATUS: COMPLETED
FROM_LOCATION_ID: 5 (R1-01 입고대기구역)
TO_LOCATION_ID: 1 (NCM-01-01 파렛트)
TRANSFER_REASON: '입고대기구역 → NCM활물질 파렛트1 적치'
```

---

### ⚙️ 시나리오 2: 생산 투입 출고 (2025-01-20)

#### 업무 흐름
1. 작업지시(WO202501200001) - 양극슬러리 생산
2. 자재출고요청: NCM811 500kg + PVDF 30kg + CNT 10kg
3. WH001 양극소재창고에서 출고
4. 양극믹싱작업장(WC001)으로 투입

#### DB 데이터

**① MCS_OUTBOUND_ORDER (출고오더)**
```sql
OUTBOUND_ID: 1
OUTBOUND_NO: OB-20250120-0001
OUTBOUND_STATUS: SHIPPED (출하완료)
WAREHOUSE_CD: WH001
REQUEST_DT: 2025-01-20 07:30:00
SHIPPED_DT: 2025-01-20 08:00:00
DESTINATION: '양극믹싱작업장(WC001)'
WO_ID: 1 (작업지시 연동)
OUTBOUND_RMK: 'WO202501200001 양극슬러리 자재투입'
```

**② MCS_OUTBOUND_ITEM (출고품목)**
```sql
-- NCM811 양극활물질
OUTBOUND_ID: 1
ITEM_CD: RM-NCM-001
LOT_NO: RM-NCM-001-20250118-001
LOCATION_ID: 1 (NCM-01-01)
REQUESTED_QTY: 500.000
ALLOCATED_QTY: 500.000
PICKED_QTY: 500.000
SHIPPED_QTY: 500.000
ITEM_STATUS: SHIPPED (출하완료)

-- PVDF 바인더
OUTBOUND_ID: 1
ITEM_CD: RM-PVD-001
LOCATION_ID: 4 (BND-01-01)
SHIPPED_QTY: 30.000

-- CNT 도전재
OUTBOUND_ID: 1
ITEM_CD: RM-CNT-001
LOCATION_ID: 4 (BND-01-01)
SHIPPED_QTY: 10.000
```

**③ MCS_LOC_TRANS_HIS (재고이력 - 출고)**
```sql
LOC_STOCK_ID: 1 (NCM-01-01 재고)
TRANS_TYPE: OB_OUT (출고출하)
TRANS_QTY: 500.000
BEFORE_QTY: 5000.000
AFTER_QTY: 4500.000
REF_TYPE: OB
REF_NO: OB-20250120-0001
TRANS_RMK: 'WO202501200001 양극믹싱 NCM811 투입'
```

---

### 🔄 시나리오 3: 생산 입고 (2025-01-22)

#### 업무 흐름
1. 양극믹싱 공정(WO202501220001) 완료
2. 양극슬러리 515kg 생산
3. WH004 전극재공창고 `SLR-01-01` 로케이션에 적치

#### DB 데이터

**① MCS_LOCATION_STOCK (생산품 재고 증가)**
```sql
LOC_STOCK_ID: 25
LOCATION_ID: 12 (SLR-01-01 슬러리 적치1)
ITEM_CD: SF-CSL-001 (NCM811 양극슬러리)
LOT_NO: SF-CSL-001-20250122-001
STOCK_QTY: 515.000
RESERVED_QTY: 0.000
AVAILABLE_QTY: 515.000
```

**② MCS_LOC_TRANS_HIS (생산입고 이력)**
```sql
LOC_STOCK_ID: 25
TRANS_TYPE: PRD_RECEIPT (생산입고)
TRANS_QTY: 515.000
BEFORE_QTY: 0.000
AFTER_QTY: 515.000
REF_NO: WO202501220001
TRANS_RMK: 'NCM811 양극슬러리 515KG 양극믹싱 생산입고 → SLR-01-01'
```

---

### 🚚 시나리오 4: 공장간 이동 (2025-01-20)

#### 업무 흐름
1. 오창1공장: 완성셀 320개 출고
2. 대전2공장: 완성셀 320개 입고
3. 모듈조립 공정에 투입 예정

#### DB 데이터

**① 오창1공장 출고 (MCS_OUTBOUND_ORDER)**
```sql
OUTBOUND_ID: (오창 출고오더)
OUTBOUND_NO: 자동생성
OUTBOUND_STATUS: SHIPPED
WAREHOUSE_CD: WH006 (완성셀창고)
DESTINATION: '대전2공장(P002) 셀입고창고(WH008)'
```

**② 대전2공장 입고 (MCS_INBOUND_ORDER)**
```sql
INBOUND_ID: (대전 입고오더)
WAREHOUSE_CD: WH008 (셀입고창고)
INBOUND_STATUS: COMPLETED
```

**③ 이동오더 (MCS_TRANSFER_ORDER는 동일 공장 내부 이동용)**

**④ 오창 재고 감소**
```sql
LOC_STOCK_ID: 13 (CEL-01-01 완성셀 파렛트)
TRANS_TYPE: OB_OUT
TRANS_QTY: 320.000
BEFORE_QTY: 1067.000
AFTER_QTY: 747.000
```

**⑤ 대전 재고 증가**
```sql
LOC_STOCK_ID: 15 (IN-01-01 셀입고 파렛트)
ITEM_CD: FG-CEL-001
STOCK_QTY: 500.000 (공장간 이동 + 추가분)
RESERVED_QTY: 100.000 (모듈조립 예약)
```

---

### 📤 시나리오 5: 고객 납품 (2025-01-22)

#### 업무 흐름
1. 영업주문(SO-2025-B006): 현대자동차 EV팩 3SET 주문
2. 대전2공장 WH011 완성팩창고에서 출고
3. 트럭 적재 후 현대차 납품

#### DB 데이터

**① MCS_OUTBOUND_ORDER (출고오더)**
```sql
OUTBOUND_ID: 2
OUTBOUND_NO: OB-20250122-0001
OUTBOUND_STATUS: SHIPPED (출하완료)
CUSTOMER_CD: V010 (현대자동차)
WAREHOUSE_CD: WH011 (완성팩창고)
REQUEST_DT: 2025-01-22 09:00:00
SHIPPED_DT: 2025-01-22 15:00:00
DESTINATION: '현대자동차(주) / 서울시 서초구'
OUTBOUND_RMK: 'EV용 배터리팩 납품 (SO-2025-B006)'
```

**② MCS_OUTBOUND_ITEM (출고품목)**
```sql
OUTBOUND_ID: 2
ITEM_CD: FG-PCK-001 (배터리팩 EV용)
LOT_NO: FG-PCK-001-20250120-001
LOCATION_ID: 28 (PCK-01-01)
REQUESTED_QTY: 3.000
ALLOCATED_QTY: 3.000
PICKED_QTY: 3.000
SHIPPED_QTY: 3.000
ITEM_STATUS: SHIPPED
```

**③ MCS_LOC_TRANS_HIS (납품 출고 이력)**
```sql
LOC_STOCK_ID: 21
TRANS_TYPE: OB_OUT
TRANS_QTY: 3.000
BEFORE_QTY: 3.000
AFTER_QTY: 0.000
REF_TYPE: OB
REF_NO: OB-20250122-0001
TRANS_RMK: '현대차 EV팩 3SET 납품'
```

---

### 🔄 시나리오 6: 로케이션 내부 이동 (2025-01-22)

#### 업무 흐름
1. 완성셀 500개를 출하대기구역으로 이동
2. `CEL-01-01` → `S1-01` (출하대기)
3. 대전공장 출하 준비

#### DB 데이터

**① MCS_TRANSFER_ORDER (이동오더)**
```sql
TRANSFER_ID: 3
TRANSFER_NO: TF-20250122-0001
TRANSFER_STATUS: IN_PROGRESS (이동중)
FROM_LOCATION_ID: 20 (CEL-01-01 완성셀 파렛트1)
TO_LOCATION_ID: 22 (S1-01 출하대기구역)
TRANSFER_REASON: '완성셀 1/20분 출하대기 이동 (대전공장 출하 준비)'
```

**② MCS_TRANSFER_ITEM (이동품목)**
```sql
TRANSFER_ID: 3
ITEM_CD: FG-CEL-001
LOT_NO: FG-CEL-001-20250120-001
TRANSFER_QTY: 500.000
ITEM_STATUS: PENDING (이동대기)
```

**③ 출발지 재고 감소 (이동 완료시)**
```sql
LOC_STOCK_ID: 13 (CEL-01-01)
TRANS_TYPE: TF_OUT (이동출발)
TRANS_QTY: 500.000
```

**④ 도착지 재고 증가 (이동 완료시)**
```sql
LOC_STOCK_ID: 22 (S1-01)
TRANS_TYPE: TF_IN (이동도착)
TRANS_QTY: 500.000
```

---

## 4. DB 테이블별 데이터 설명

### 📍 MCS_ZONE (구역 정보) - 22건

창고를 용도별로 나눈 구역입니다.

| ZONE_ID | 창고 | 구역코드 | 구역명 | 구역유형 |
|---------|------|----------|--------|----------|
| 1 | WH001 | Z-NCM | NCM활물질 보관구역 | STORAGE |
| 3 | WH001 | Z-R1 | 입고 대기구역 | RECEIVING |
| 6 | WH003 | Z-ELY | 전해액 보관구역(방폭) | STORAGE |
| 8 | WH003 | Z-QC1 | 원자재 검수구역 | QC |
| 15 | WH006 | Z-S1 | 출하 대기구역 (→대전) | SHIPPING |
| 22 | WH011 | Z-S2 | 출하 대기구역 | SHIPPING |

**구역유형 (ZONE_TYPE)**
- `STORAGE`: 보관구역 (일반 재고 보관)
- `RECEIVING`: 입고구역 (입고 임시 보관)
- `SHIPPING`: 출하구역 (출하 준비)
- `STAGING`: 임시구역 (작업 대기)
- `QC`: 검수구역 (품질 검사)

---

### 📦 MCS_LOCATION (로케이션 정보) - 30건

구역 내 실제 적치 위치입니다.

| LOCATION_ID | 구역 | 로케이션코드 | 로케이션명 | 최대수용량 | 현재사용량 | 상태 |
|-------------|------|-------------|-----------|-----------|-----------|------|
| 1 | Z-NCM | NCM-01-01 | NCM811 파렛트1 | 10000 | 3500 | PARTIAL |
| 8 | Z-ELY | ELY-01-01 | 전해액 탱크1 | 5000 | 2600 | PARTIAL |
| 12 | Z-SLR | SLR-01-01 | 슬러리/전극 적치1 | 2000 | 515 | PARTIAL |
| 20 | Z-CEL | CEL-01-01 | 완성셀 파렛트1 | 3000 | 1067 | PARTIAL |
| 28 | Z-PCK | PCK-01-01 | 완성팩 적치1 (EV) | 50 | 7 | PARTIAL |

**로케이션상태 (LOCATION_STATUS)**
- `EMPTY`: 비어있음 (사용량 0%)
- `PARTIAL`: 일부사용 (0% < 사용량 < 100%)
- `FULL`: 가득찼음 (사용량 100%)
- `BLOCKED`: 사용불가 (점검/폐쇄)

---

### 📊 MCS_LOCATION_STOCK (로케이션 재고) - 29건

로케이션별 품목/LOT 단위 재고입니다.

| LOC_STOCK_ID | 로케이션 | 품목 | LOT | 재고수량 | 예약수량 | 가용수량 |
|--------------|---------|------|-----|---------|---------|---------|
| 1 | NCM-01-01 | RM-NCM-001 | RM-NCM-001-20250118-001 | 3500 | 500 | 3000 |
| 8 | ELY-01-01 | RM-ELY-001 | RM-ELY-001-20250118-001 | 2600 | 200 | 2400 |
| 13 | CEL-01-01 | FG-CEL-001 | FG-CEL-001-20250120-001 | 1067 | 320 | 747 |
| 21 | PCK-01-01 | FG-PCK-001 | FG-PCK-001-20250120-001 | 3 | 0 | 3 |
| 25 | SLR-01-01 | SF-CSL-001 | SF-CSL-001-20250122-001 | 515 | 0 | 515 |

**핵심 컬럼**
- `STOCK_QTY`: 실제 재고 수량
- `RESERVED_QTY`: 예약된 수량 (출고/생산 예정)
- `AVAILABLE_QTY`: 가용 재고 = STOCK_QTY - RESERVED_QTY (자동계산)

---

### 📝 MCS_LOC_TRANS_HIS (로케이션 재고 이력) - 24건

모든 재고 변동 이력을 기록합니다.

| LOC_TRANS_ID | 재고ID | 거래유형 | 변동수량 | 변경전 | 변경후 | 참조번호 | 비고 |
|--------------|--------|---------|---------|-------|-------|---------|------|
| 1 | 1 | IB_IN | 5000 | 0 | 5000 | IB-20250118-0001 | NCM811 5톤 입고 |
| 9 | 1 | OB_OUT | 500 | 5000 | 4500 | OB-20250120-0001 | 양극믹싱 투입 |
| 17 | 25 | PRD_RECEIPT | 515 | 0 | 515 | WO202501220001 | 양극슬러리 생산입고 |
| 14 | 21 | OB_OUT | 3 | 3 | 0 | OB-20250122-0001 | 현대차 납품 |

**거래유형 (TRANS_TYPE)**
- `IB_IN`: 입고적치
- `OB_OUT`: 출고출하
- `TF_OUT`: 이동출발
- `TF_IN`: 이동도착
- `PRD_ISSUE`: 생산투입
- `PRD_RECEIPT`: 생산입고
- `ADJ_PLUS`: 조정증가
- `ADJ_MINUS`: 조정감소

---

### 📥 MCS_INBOUND_ORDER (입고 오더) - 11건

입고 단위 관리 헤더입니다.

| INBOUND_ID | 입고번호 | 상태 | 거래처 | 창고 | 예정일 | 실제일시 | 비고 |
|-----------|---------|------|--------|------|--------|---------|------|
| 1 | IB-20250118-0001 | COMPLETED | V001 | WH001 | 1/18 | 1/18 08:30 | NCM811 5톤 입고 |
| 7 | IB-20250125-0001 | INSPECTING | V004 | WH003 | 1/25 | 1/25 08:30 | 전해액 2000L (검수중) |
| 8 | IB-20250128-0001 | ARRIVED | V001 | WH001 | 1/28 | 1/28 14:00 | NCM811 3톤 (도착) |
| 9 | IB-20250130-0001 | PLANNED | V009 | WH009 | 1/30 | NULL | BMS 200개 (예정) |
| 11 | IB-20250120-0001 | CANCELLED | V006 | WH001 | 1/20 | NULL | PVDF (취소) |

**입고상태 (INBOUND_STATUS)**
- `PLANNED`: 입고예정 (아직 도착 안 함)
- `ARRIVED`: 도착 (검수 대기)
- `INSPECTING`: 검수중 (품질 검사)
- `COMPLETED`: 완료 (적치 완료)
- `CANCELLED`: 취소

---

### 📦 MCS_INBOUND_ITEM (입고 품목) - 15건

입고 오더의 품목별 상세입니다.

| INBOUND_ITEM_ID | 입고ID | 품목 | LOT | 로케이션 | 예정수량 | 실제수량 | 상태 | 비고 |
|----------------|--------|------|-----|---------|---------|---------|------|------|
| 1 | 1 | RM-NCM-001 | RM-NCM-001-20250118-001 | NCM-01-01 | 5000 | 5000 | STOCKED | 에코프로 LOT |
| 7 | 7 | RM-ELY-001 | NULL | NULL | 2000 | 0 | PENDING | 순도검사 진행중 |
| 8 | 8 | RM-NCM-001 | NULL | NULL | 3000 | 0 | PENDING | 입도분포 검사 대기 |

**품목상태 (ITEM_STATUS)**
- `PENDING`: 대기 (검수 또는 적치 대기)
- `INSPECTED`: 검수완료
- `STOCKED`: 적치완료
- `REJECTED`: 반품

---

### 📤 MCS_OUTBOUND_ORDER (출고 오더) - 6건

출고 단위 관리 헤더입니다.

| OUTBOUND_ID | 출고번호 | 상태 | 고객 | 창고 | 목적지 | 작업지시 | 비고 |
|------------|---------|------|------|------|--------|---------|------|
| 1 | OB-20250120-0001 | SHIPPED | NULL | WH001 | 양극믹싱작업장 | WO#1 | 생산투입 |
| 2 | OB-20250122-0001 | SHIPPED | V010 | WH011 | 현대차 | NULL | EV팩 납품 |
| 3 | OB-20250122-0002 | PICKED | NULL | WH003 | 전해액주입작업장 | WO#34 | 피킹완료 |
| 4 | OB-20250122-0003 | ALLOCATED | NULL | WH006 | 대전2공장 | NULL | 할당완료 |
| 5 | OB-20250125-0001 | REQUESTED | V011 | WH011 | 기아 | NULL | 출고요청 |
| 6 | OB-20250120-0001 | CANCELLED | V012 | WH011 | NULL | NULL | 취소 |

**출고상태 (OUTBOUND_STATUS)**
- `REQUESTED`: 출고요청 (피킹 전)
- `ALLOCATED`: 할당완료 (재고 예약됨)
- `PICKING`: 피킹중 (작업 진행중)
- `PICKED`: 피킹완료 (출하 대기)
- `SHIPPED`: 출하완료 (배송 시작)
- `CANCELLED`: 취소

---

### 📦 MCS_OUTBOUND_ITEM (출고 품목) - 9건

출고 오더의 품목별 상세입니다.

| OUTBOUND_ITEM_ID | 출고ID | 품목 | LOT | 로케이션 | 요청 | 할당 | 피킹 | 출하 | 상태 |
|-----------------|--------|------|-----|---------|-----|-----|-----|-----|------|
| 1 | 1 | RM-NCM-001 | RM-NCM-001-20250118-001 | NCM-01-01 | 500 | 500 | 500 | 500 | SHIPPED |
| 4 | 2 | FG-PCK-001 | FG-PCK-001-20250120-001 | PCK-01-01 | 3 | 3 | 3 | 3 | SHIPPED |
| 5 | 3 | RM-ELY-001 | RM-ELY-001-20250118-001 | ELY-01-01 | 200 | 200 | 200 | 0 | PICKED |
| 6 | 4 | FG-CEL-001 | FG-CEL-001-20250120-001 | CEL-01-01 | 320 | 320 | 0 | 0 | ALLOCATED |
| 8 | 5 | FG-PCK-001 | FG-PCK-001-20250121-001 | PCK-01-01 | 4 | 0 | 0 | 0 | PENDING |

**수량 진행 단계**
```
REQUESTED_QTY (요청수량)
  ↓ 재고 할당
ALLOCATED_QTY (할당수량)
  ↓ 물리적 피킹
PICKED_QTY (피킹수량)
  ↓ 출하 처리
SHIPPED_QTY (출하수량)
```

---

### 🔄 MCS_TRANSFER_ORDER (이동 오더) - 5건

로케이션 간 이동 관리입니다.

| TRANSFER_ID | 이동번호 | 상태 | 출발 로케이션 | 도착 로케이션 | 이동사유 |
|------------|---------|------|--------------|--------------|---------|
| 1 | TF-20250118-0001 | COMPLETED | R1-01 (입고대기) | NCM-01-01 (파렛트) | 입고 → 정위치 |
| 2 | TF-20250118-0002 | COMPLETED | QC1-01 (검수) | ELY-01-01 (탱크) | 검수 → 적치 |
| 3 | TF-20250122-0001 | IN_PROGRESS | CEL-01-01 (파렛트) | S1-01 (출하대기) | 대전 출하준비 |
| 4 | TF-20250125-0001 | REQUESTED | PCK-01-01 (적치) | S2-01 (출하대기) | 현대차 납품준비 |
| 5 | TF-20250121-0001 | CANCELLED | WET-01-01 (웻셀) | STG-01 (임시) | 취소 |

**이동상태 (TRANSFER_STATUS)**
- `REQUESTED`: 이동요청
- `IN_PROGRESS`: 이동중
- `COMPLETED`: 완료
- `CANCELLED`: 취소

---

### 📦 MCS_TRANSFER_ITEM (이동 품목) - 5건

이동 오더의 품목별 상세입니다.

| TRANSFER_ITEM_ID | 이동ID | 품목 | LOT | 이동수량 | 상태 |
|-----------------|--------|------|-----|---------|------|
| 1 | 1 | RM-NCM-001 | RM-NCM-001-20250118-001 | 5000 | MOVED |
| 2 | 2 | RM-ELY-001 | RM-ELY-001-20250118-001 | 3000 | MOVED |
| 3 | 3 | FG-CEL-001 | FG-CEL-001-20250120-001 | 500 | PENDING |
| 4 | 4 | FG-PCK-001 | FG-PCK-001-20250121-001 | 4 | PENDING |
| 5 | 5 | SF-WET-001 | SF-WET-001-20250121-001 | 800 | CANCELLED |

---

## 5. 재고 현황 조회

### 5.1 로케이션별 재고 조회 쿼리

```sql
SELECT
    p.PLANT_NM AS '공장',
    wh.WAREHOUSE_NM AS '창고',
    z.ZONE_NM AS '구역',
    l.LOCATION_CD AS '로케이션코드',
    i.ITEM_NM AS '품목명',
    ls.LOT_NO AS 'LOT번호',
    ls.STOCK_QTY AS '재고수량',
    ls.RESERVED_QTY AS '예약수량',
    ls.AVAILABLE_QTY AS '가용수량',
    i.UNIT AS '단위'
FROM MCS_LOCATION_STOCK ls
    JOIN MCS_LOCATION l ON ls.LOCATION_ID = l.LOCATION_ID
    JOIN MCS_ZONE z ON l.ZONE_ID = z.ZONE_ID
    JOIN MST_WAREHOUSE wh ON z.WAREHOUSE_CD = wh.WAREHOUSE_CD
    JOIN MST_PLANT p ON ls.PLANT_CD = p.PLANT_CD
    JOIN MST_ITEM i ON ls.ITEM_CD = i.ITEM_CD
WHERE ls.STOCK_QTY > 0
ORDER BY p.PLANT_CD, wh.WAREHOUSE_CD, z.ZONE_CD, l.LOCATION_CD;
```

### 5.2 재고 이력 조회 쿼리

```sql
SELECT
    h.REG_DTM AS '일시',
    l.LOCATION_CD AS '로케이션',
    i.ITEM_NM AS '품목',
    ls.LOT_NO AS 'LOT',
    CASE h.TRANS_TYPE
        WHEN 'IB_IN' THEN '입고적치'
        WHEN 'OB_OUT' THEN '출고출하'
        WHEN 'PRD_ISSUE' THEN '생산투입'
        WHEN 'PRD_RECEIPT' THEN '생산입고'
        WHEN 'TF_IN' THEN '이동도착'
        WHEN 'TF_OUT' THEN '이동출발'
        WHEN 'ADJ_PLUS' THEN '조정증가'
        WHEN 'ADJ_MINUS' THEN '조정감소'
    END AS '거래유형',
    h.TRANS_QTY AS '변동수량',
    h.BEFORE_QTY AS '변경전',
    h.AFTER_QTY AS '변경후',
    h.REF_NO AS '참조번호',
    h.TRANS_RMK AS '비고'
FROM MCS_LOC_TRANS_HIS h
    JOIN MCS_LOCATION_STOCK ls ON h.LOC_STOCK_ID = ls.LOC_STOCK_ID
    JOIN MCS_LOCATION l ON ls.LOCATION_ID = l.LOCATION_ID
    JOIN MST_ITEM i ON ls.ITEM_CD = i.ITEM_CD
ORDER BY h.REG_DTM DESC
LIMIT 20;
```

### 5.3 입출고 현황 조회 쿼리

```sql
-- 입고 현황
SELECT
    io.INBOUND_NO AS '입고번호',
    io.INBOUND_STATUS AS '상태',
    v.VENDOR_NM AS '거래처',
    wh.WAREHOUSE_NM AS '창고',
    ii.ITEM_CD AS '품목코드',
    i.ITEM_NM AS '품목명',
    ii.EXPECTED_QTY AS '예정수량',
    ii.ACTUAL_QTY AS '실제수량',
    ii.ITEM_STATUS AS '품목상태',
    l.LOCATION_CD AS '적치위치'
FROM MCS_INBOUND_ORDER io
    JOIN MCS_INBOUND_ITEM ii ON io.INBOUND_ID = ii.INBOUND_ID
    LEFT JOIN MST_VENDOR v ON io.VENDOR_CD = v.VENDOR_CD
    JOIN MST_WAREHOUSE wh ON io.WAREHOUSE_CD = wh.WAREHOUSE_CD
    JOIN MST_ITEM i ON ii.ITEM_CD = i.ITEM_CD
    LEFT JOIN MCS_LOCATION l ON ii.LOCATION_ID = l.LOCATION_ID
ORDER BY io.EXPECTED_DT DESC;

-- 출고 현황
SELECT
    oo.OUTBOUND_NO AS '출고번호',
    oo.OUTBOUND_STATUS AS '상태',
    COALESCE(v.VENDOR_NM, oo.DESTINATION) AS '목적지',
    oi.ITEM_CD AS '품목코드',
    i.ITEM_NM AS '품목명',
    oi.REQUESTED_QTY AS '요청수량',
    oi.ALLOCATED_QTY AS '할당수량',
    oi.PICKED_QTY AS '피킹수량',
    oi.SHIPPED_QTY AS '출하수량',
    oi.ITEM_STATUS AS '품목상태',
    l.LOCATION_CD AS '출고위치'
FROM MCS_OUTBOUND_ORDER oo
    JOIN MCS_OUTBOUND_ITEM oi ON oo.OUTBOUND_ID = oi.OUTBOUND_ID
    LEFT JOIN MST_VENDOR v ON oo.CUSTOMER_CD = v.VENDOR_CD
    JOIN MST_ITEM i ON oi.ITEM_CD = i.ITEM_CD
    LEFT JOIN MCS_LOCATION l ON oi.LOCATION_ID = l.LOCATION_ID
ORDER BY oo.REQUEST_DT DESC;
```

---

## 📊 부록: 재고 현황 요약 (2025-01-22 기준)

### 오창1공장 주요 재고

| 창고 | 품목 | 재고 | 예약 | 가용 | 단위 |
|------|------|------|------|------|------|
| WH001 | NCM811 양극활물질 | 3,500 | 500 | 3,000 | KG |
| WH001 | 파우치필름 | 8,500 | 500 | 8,000 | EA |
| WH002 | 음극활물질 | 4,200 | 400 | 3,800 | KG |
| WH003 | 전해액 | 2,600 | 200 | 2,400 | L |
| WH003 | NMP 용매 | 1,600 | 0 | 1,600 | L |
| WH004 | 양극슬러리 | 515 | 0 | 515 | KG |
| WH004 | 양극(슬리팅) | 2,940 | 1,000 | 1,940 | EA |
| WH005 | 젤리롤 | 1,500 | 500 | 1,000 | EA |
| WH006 | 완성셀(1/20) | 1,067 | 320 | 747 | EA |
| WH006 | 완성셀(1/21) | 1,164 | 352 | 812 | EA |

### 대전2공장 주요 재고

| 창고 | 품목 | 재고 | 예약 | 가용 | 단위 |
|------|------|------|------|------|------|
| WH008 | 완성셀 | 500 | 100 | 400 | EA |
| WH009 | BMS기판(EV) | 138 | 25 | 113 | EA |
| WH009 | 버스바 | 4,070 | 375 | 3,695 | EA |
| WH010 | 모듈(16S1P) | 18 | 6 | 12 | SET |
| WH011 | EV팩(1/20) | 3 | 0 | 3 | SET |
| WH011 | EV팩(1/21) | 4 | 0 | 4 | SET |
| WH011 | ESS팩(1/21) | 3 | 0 | 3 | SET |

---

**문서 버전**: v1.0
**작성일**: 2025-01-22
**작성자**: MCS 시스템 설계팀
