# MES 시스템 데이터베이스 설계서

## 1. 개요

### 1.1 시스템 구성
- DBMS: MariaDB 10.x
- 문자셋: UTF8MB4
- 정렬: utf8mb4_general_ci

### 1.2 테이블 명명 규칙
- 기준정보: `MST_` (Master)
- 생산계획: `PLN_` (Plan)
- 생산실적: `PRD_` (Production)
- 품질관리: `QC_` (Quality Control)
- 재고관리: `INV_` (Inventory)
- 설비관리: `EQP_` (Equipment)
- 공통코드: `COM_` (Common)

### 1.3 컬럼 명명 규칙
- PK: `테이블약어_ID`
- FK: 참조테이블의 PK명 그대로 사용
- 일자: `_DT` (DATE)
- 일시: `_DTM` (DATETIME)
- 수량: `_QTY`
- 코드: `_CD`
- 명칭: `_NM`
- 비고: `_RMK` (Remark)
- 등록: `REG_`
- 수정: `UPD_`

---

## 2. 엔티티 관계도 (ERD)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              【 기준정보 영역 】                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐                  │
│  │  MST_COMPANY │      │  MST_PLANT   │      │  MST_DEPT    │                  │
│  │  (회사정보)   │──1:N──│  (공장정보)   │──1:N──│  (부서정보)   │                  │
│  └──────────────┘      └──────────────┘      └──────────────┘                  │
│                              │                      │                          │
│                              │1:N                   │1:N                       │
│                              ▼                      ▼                          │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐                  │
│  │  MST_ITEM    │      │ MST_WORKCENTER│     │  MST_WORKER  │                  │
│  │  (품목정보)   │      │  (작업장정보)  │      │  (작업자정보) │                  │
│  └──────────────┘      └──────────────┘      └──────────────┘                  │
│         │                    │                                                 │
│         │1:N                 │1:N                                              │
│         ▼                    ▼                                                 │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐                  │
│  │  MST_BOM     │      │ MST_EQUIPMENT│      │ MST_ROUTING  │                  │
│  │  (BOM정보)   │      │  (설비정보)   │      │  (공정정보)   │                  │
│  └──────────────┘      └──────────────┘      └──────────────┘                  │
│         │                    │                      │                          │
└─────────┼────────────────────┼──────────────────────┼──────────────────────────┘
          │                    │                      │
          ▼                    ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              【 생산계획 영역 】                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────┐      ┌──────────────┐                                        │
│  │ PLN_PROD_PLAN│──1:N──│PLN_WORK_ORDER│                                        │
│  │  (생산계획)   │      │  (작업지시)   │                                        │
│  └──────────────┘      └──────────────┘                                        │
│                              │                                                 │
└──────────────────────────────┼─────────────────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│  【 생산실적 영역 】  │ │  【 품질관리 영역 】  │ │  【 재고관리 영역 】  │
├─────────────────────┤ ├─────────────────────┤ ├─────────────────────┤
│                     │ │                     │ │                     │
│ ┌─────────────────┐ │ │ ┌─────────────────┐ │ │ ┌─────────────────┐ │
│ │ PRD_WORK_RESULT │ │ │ │ QC_INSPECT_STD  │ │ │ │   INV_STOCK     │ │
│ │   (작업실적)     │ │ │ │   (검사기준)     │ │ │ │   (재고현황)     │ │
│ └─────────────────┘ │ │ └─────────────────┘ │ │ └─────────────────┘ │
│         │           │ │         │           │ │         ▲           │
│         │1:N        │ │         │1:N        │ │         │           │
│         ▼           │ │         ▼           │ │         │           │
│ ┌─────────────────┐ │ │ ┌─────────────────┐ │ │ ┌─────────────────┐ │
│ │PRD_PROCESS_RESULT│ │ │ │QC_INSPECT_RESULT│ │ │ │  INV_TRANS_HIS  │ │
│ │   (공정실적)     │ │ │ │   (검사실적)     │ │ │ │   (입출고이력)   │ │
│ └─────────────────┘ │ │ └─────────────────┘ │ │ └─────────────────┘ │
│         │           │ │         │           │ │         │           │
│         │1:N        │ │         │1:N        │ │         │1:N        │
│         ▼           │ │         ▼           │ │         ▼           │
│ ┌─────────────────┐ │ │ ┌─────────────────┐ │ │ ┌─────────────────┐ │
│ │ PRD_DEFECT_HIS  │ │ │ │ QC_DEFECT_HIS   │ │ │ │   INV_LOT       │ │
│ │   (불량이력)     │ │ │ │   (불량이력)     │ │ │ │   (LOT정보)     │ │
│ └─────────────────┘ │ │ └─────────────────┘ │ │ └─────────────────┘ │
│                     │ │                     │ │                     │
└─────────────────────┘ └─────────────────────┘ └─────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              【 설비관리 영역 】                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐                  │
│  │EQP_OPER_STATUS│     │EQP_DOWNTIME  │      │ EQP_MAINT_HIS│                  │
│  │  (가동현황)   │      │  (비가동이력) │      │  (정비이력)   │                  │
│  └──────────────┘      └──────────────┘      └──────────────┘                  │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 테이블 상세 설계

### 3.1 공통코드 테이블

#### COM_CODE_GRP (공통코드그룹)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| GRP_CD | VARCHAR(20) | NO | PK | 그룹코드 |
| GRP_NM | VARCHAR(100) | NO | | 그룹명 |
| GRP_DESC | VARCHAR(500) | YES | | 그룹설명 |
| USE_YN | CHAR(1) | NO | | 사용여부 (Y/N) |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### COM_CODE (공통코드)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| GRP_CD | VARCHAR(20) | NO | PK,FK | 그룹코드 |
| COM_CD | VARCHAR(20) | NO | PK | 공통코드 |
| COM_NM | VARCHAR(100) | NO | | 코드명 |
| COM_DESC | VARCHAR(500) | YES | | 코드설명 |
| SORT_SEQ | INT | NO | | 정렬순서 |
| ATTR1 | VARCHAR(100) | YES | | 속성1 |
| ATTR2 | VARCHAR(100) | YES | | 속성2 |
| ATTR3 | VARCHAR(100) | YES | | 속성3 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

---

### 3.2 기준정보 테이블

#### MST_COMPANY (회사정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| COMPANY_CD | VARCHAR(20) | NO | PK | 회사코드 |
| COMPANY_NM | VARCHAR(100) | NO | | 회사명 |
| BIZ_NO | VARCHAR(20) | YES | | 사업자번호 |
| CEO_NM | VARCHAR(50) | YES | | 대표자명 |
| ADDR | VARCHAR(500) | YES | | 주소 |
| TEL_NO | VARCHAR(20) | YES | | 전화번호 |
| FAX_NO | VARCHAR(20) | YES | | 팩스번호 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_PLANT (공장정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| PLANT_CD | VARCHAR(20) | NO | PK | 공장코드 |
| COMPANY_CD | VARCHAR(20) | NO | FK | 회사코드 |
| PLANT_NM | VARCHAR(100) | NO | | 공장명 |
| ADDR | VARCHAR(500) | YES | | 주소 |
| TEL_NO | VARCHAR(20) | YES | | 전화번호 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_DEPT (부서정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| DEPT_CD | VARCHAR(20) | NO | PK | 부서코드 |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| DEPT_NM | VARCHAR(100) | NO | | 부서명 |
| PARENT_DEPT_CD | VARCHAR(20) | YES | FK | 상위부서코드 |
| DEPT_LEVEL | INT | NO | | 부서레벨 |
| SORT_SEQ | INT | NO | | 정렬순서 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_WORKER (작업자정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| WORKER_ID | VARCHAR(20) | NO | PK | 작업자ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| DEPT_CD | VARCHAR(20) | YES | FK | 부서코드 |
| WORKER_NM | VARCHAR(50) | NO | | 작업자명 |
| WORKER_TYPE | VARCHAR(20) | NO | | 작업자유형 (정규직/계약직/파견) |
| POSITION | VARCHAR(50) | YES | | 직위 |
| HIRE_DT | DATE | YES | | 입사일 |
| MOBILE_NO | VARCHAR(20) | YES | | 휴대폰번호 |
| EMAIL | VARCHAR(100) | YES | | 이메일 |
| SKILL_LEVEL | VARCHAR(20) | YES | | 숙련도 (초급/중급/고급) |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_WORKCENTER (작업장정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| WORKCENTER_CD | VARCHAR(20) | NO | PK | 작업장코드 |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| WORKCENTER_NM | VARCHAR(100) | NO | | 작업장명 |
| WORKCENTER_TYPE | VARCHAR(20) | NO | | 작업장유형 |
| LOCATION | VARCHAR(100) | YES | | 위치 |
| CAPACITY_QTY | DECIMAL(15,3) | YES | | 생산능력 |
| CAPACITY_UNIT | VARCHAR(20) | YES | | 능력단위 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_ITEM (품목정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| ITEM_CD | VARCHAR(50) | NO | PK | 품목코드 |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| ITEM_NM | VARCHAR(200) | NO | | 품목명 |
| ITEM_SPEC | VARCHAR(500) | YES | | 규격 |
| ITEM_TYPE | VARCHAR(20) | NO | | 품목유형 (원자재/반제품/완제품) |
| ITEM_GRP | VARCHAR(50) | YES | | 품목그룹 |
| UNIT | VARCHAR(20) | NO | | 기본단위 |
| SAFETY_STOCK_QTY | DECIMAL(15,3) | YES | | 안전재고수량 |
| LEAD_TIME | INT | YES | | 리드타임(일) |
| LOT_SIZE | DECIMAL(15,3) | YES | | LOT크기 |
| SHELF_LIFE | INT | YES | | 유효기간(일) |
| WEIGHT | DECIMAL(15,5) | YES | | 중량 |
| WEIGHT_UNIT | VARCHAR(20) | YES | | 중량단위 |
| PURCHASE_PRICE | DECIMAL(18,2) | YES | | 구매단가 |
| SALE_PRICE | DECIMAL(18,2) | YES | | 판매단가 |
| VENDOR_CD | VARCHAR(50) | YES | | 주거래처코드 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_BOM (BOM정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| BOM_ID | BIGINT | NO | PK,AI | BOM ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| PARENT_ITEM_CD | VARCHAR(50) | NO | FK | 모품목코드 |
| CHILD_ITEM_CD | VARCHAR(50) | NO | FK | 자품목코드 |
| BOM_LEVEL | INT | NO | | BOM레벨 |
| BOM_QTY | DECIMAL(15,5) | NO | | 소요량 |
| LOSS_RATE | DECIMAL(5,2) | YES | | 손실율(%) |
| START_DT | DATE | NO | | 유효시작일 |
| END_DT | DATE | YES | | 유효종료일 |
| BOM_RMK | VARCHAR(500) | YES | | 비고 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_ROUTING (공정정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| ROUTING_ID | BIGINT | NO | PK,AI | 라우팅ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| ITEM_CD | VARCHAR(50) | NO | FK | 품목코드 |
| PROCESS_SEQ | INT | NO | | 공정순서 |
| PROCESS_CD | VARCHAR(20) | NO | | 공정코드 |
| PROCESS_NM | VARCHAR(100) | NO | | 공정명 |
| WORKCENTER_CD | VARCHAR(20) | YES | FK | 작업장코드 |
| EQUIPMENT_CD | VARCHAR(20) | YES | FK | 설비코드 |
| SETUP_TIME | DECIMAL(10,2) | YES | | 준비시간(분) |
| RUN_TIME | DECIMAL(10,2) | YES | | 가공시간(분) |
| WAIT_TIME | DECIMAL(10,2) | YES | | 대기시간(분) |
| MOVE_TIME | DECIMAL(10,2) | YES | | 이동시간(분) |
| PROCESS_DESC | VARCHAR(1000) | YES | | 공정설명 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_EQUIPMENT (설비정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| EQUIPMENT_CD | VARCHAR(20) | NO | PK | 설비코드 |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| WORKCENTER_CD | VARCHAR(20) | NO | FK | 작업장코드 |
| EQUIPMENT_NM | VARCHAR(100) | NO | | 설비명 |
| EQUIPMENT_TYPE | VARCHAR(50) | NO | | 설비유형 |
| MODEL_NM | VARCHAR(100) | YES | | 모델명 |
| MAKER | VARCHAR(100) | YES | | 제조사 |
| SERIAL_NO | VARCHAR(100) | YES | | 시리얼번호 |
| INSTALL_DT | DATE | YES | | 설치일 |
| PURCHASE_DT | DATE | YES | | 구매일 |
| PURCHASE_PRICE | DECIMAL(18,2) | YES | | 구매가격 |
| CAPACITY_QTY | DECIMAL(15,3) | YES | | 생산능력 |
| CAPACITY_UNIT | VARCHAR(20) | YES | | 능력단위 |
| EQUIPMENT_STATUS | VARCHAR(20) | NO | | 설비상태 (가동/비가동/정비중) |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_VENDOR (거래처정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| VENDOR_CD | VARCHAR(50) | NO | PK | 거래처코드 |
| VENDOR_NM | VARCHAR(200) | NO | | 거래처명 |
| VENDOR_TYPE | VARCHAR(20) | NO | | 거래처유형 (매입처/매출처/둘다) |
| BIZ_NO | VARCHAR(20) | YES | | 사업자번호 |
| CEO_NM | VARCHAR(50) | YES | | 대표자명 |
| BIZ_TYPE | VARCHAR(100) | YES | | 업태 |
| BIZ_ITEM | VARCHAR(100) | YES | | 종목 |
| ADDR | VARCHAR(500) | YES | | 주소 |
| TEL_NO | VARCHAR(20) | YES | | 전화번호 |
| FAX_NO | VARCHAR(20) | YES | | 팩스번호 |
| EMAIL | VARCHAR(100) | YES | | 이메일 |
| MANAGER_NM | VARCHAR(50) | YES | | 담당자명 |
| MANAGER_TEL | VARCHAR(20) | YES | | 담당자연락처 |
| PAYMENT_TERMS | VARCHAR(100) | YES | | 결제조건 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### MST_WAREHOUSE (창고정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| WAREHOUSE_CD | VARCHAR(20) | NO | PK | 창고코드 |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| WAREHOUSE_NM | VARCHAR(100) | NO | | 창고명 |
| WAREHOUSE_TYPE | VARCHAR(20) | NO | | 창고유형 (원자재/반제품/완제품/불량) |
| LOCATION | VARCHAR(100) | YES | | 위치 |
| CAPACITY | DECIMAL(15,3) | YES | | 수용능력 |
| MANAGER_ID | VARCHAR(20) | YES | | 담당자ID |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

---

### 3.3 생산계획 테이블

#### PLN_PROD_PLAN (생산계획)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| PLAN_ID | BIGINT | NO | PK,AI | 계획ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| PLAN_NO | VARCHAR(30) | NO | UK | 계획번호 |
| PLAN_DT | DATE | NO | | 계획일자 |
| PLAN_TYPE | VARCHAR(20) | NO | | 계획유형 (일간/주간/월간) |
| ITEM_CD | VARCHAR(50) | NO | FK | 품목코드 |
| PLAN_QTY | DECIMAL(15,3) | NO | | 계획수량 |
| PLAN_START_DT | DATE | NO | | 계획시작일 |
| PLAN_END_DT | DATE | NO | | 계획종료일 |
| PRIORITY | INT | YES | | 우선순위 |
| ORDER_NO | VARCHAR(50) | YES | | 수주번호 |
| CUSTOMER_CD | VARCHAR(50) | YES | | 고객코드 |
| PLAN_STATUS | VARCHAR(20) | NO | | 계획상태 (계획/확정/진행/완료/취소) |
| PLAN_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### PLN_WORK_ORDER (작업지시)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| WO_ID | BIGINT | NO | PK,AI | 작업지시ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| WO_NO | VARCHAR(30) | NO | UK | 작업지시번호 |
| PLAN_ID | BIGINT | YES | FK | 계획ID |
| WO_DT | DATE | NO | | 작업지시일 |
| ITEM_CD | VARCHAR(50) | NO | FK | 품목코드 |
| WO_QTY | DECIMAL(15,3) | NO | | 지시수량 |
| WORKCENTER_CD | VARCHAR(20) | YES | FK | 작업장코드 |
| EQUIPMENT_CD | VARCHAR(20) | YES | FK | 설비코드 |
| WORKER_ID | VARCHAR(20) | YES | FK | 작업자ID |
| PLAN_START_DTM | DATETIME | NO | | 계획시작일시 |
| PLAN_END_DTM | DATETIME | NO | | 계획종료일시 |
| ACTUAL_START_DTM | DATETIME | YES | | 실제시작일시 |
| ACTUAL_END_DTM | DATETIME | YES | | 실제종료일시 |
| GOOD_QTY | DECIMAL(15,3) | YES | | 양품수량 |
| DEFECT_QTY | DECIMAL(15,3) | YES | | 불량수량 |
| WO_STATUS | VARCHAR(20) | NO | | 작업상태 (대기/진행/완료/취소) |
| PRIORITY | INT | YES | | 우선순위 |
| LOT_NO | VARCHAR(50) | YES | | LOT번호 |
| WO_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

---

### 3.4 생산실적 테이블

#### PRD_WORK_RESULT (작업실적)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| RESULT_ID | BIGINT | NO | PK,AI | 실적ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| WO_ID | BIGINT | NO | FK | 작업지시ID |
| RESULT_DT | DATE | NO | | 실적일자 |
| SHIFT | VARCHAR(10) | NO | | 근무조 (주간/야간) |
| WORKER_ID | VARCHAR(20) | NO | FK | 작업자ID |
| WORKCENTER_CD | VARCHAR(20) | NO | FK | 작업장코드 |
| EQUIPMENT_CD | VARCHAR(20) | YES | FK | 설비코드 |
| ITEM_CD | VARCHAR(50) | NO | FK | 품목코드 |
| PROD_QTY | DECIMAL(15,3) | NO | | 생산수량 |
| GOOD_QTY | DECIMAL(15,3) | NO | | 양품수량 |
| DEFECT_QTY | DECIMAL(15,3) | YES | | 불량수량 |
| START_DTM | DATETIME | NO | | 시작일시 |
| END_DTM | DATETIME | YES | | 종료일시 |
| WORK_TIME | DECIMAL(10,2) | YES | | 작업시간(분) |
| LOT_NO | VARCHAR(50) | YES | | LOT번호 |
| RESULT_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### PRD_PROCESS_RESULT (공정실적)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| PROC_RESULT_ID | BIGINT | NO | PK,AI | 공정실적ID |
| RESULT_ID | BIGINT | NO | FK | 작업실적ID |
| ROUTING_ID | BIGINT | NO | FK | 라우팅ID |
| PROCESS_SEQ | INT | NO | | 공정순서 |
| PROCESS_CD | VARCHAR(20) | NO | | 공정코드 |
| START_DTM | DATETIME | NO | | 시작일시 |
| END_DTM | DATETIME | YES | | 종료일시 |
| INPUT_QTY | DECIMAL(15,3) | NO | | 투입수량 |
| OUTPUT_QTY | DECIMAL(15,3) | YES | | 산출수량 |
| DEFECT_QTY | DECIMAL(15,3) | YES | | 불량수량 |
| WORKER_ID | VARCHAR(20) | YES | FK | 작업자ID |
| EQUIPMENT_CD | VARCHAR(20) | YES | FK | 설비코드 |
| PROCESS_STATUS | VARCHAR(20) | NO | | 공정상태 (대기/진행/완료) |
| PROC_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### PRD_DEFECT_HIS (불량이력)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| DEFECT_ID | BIGINT | NO | PK,AI | 불량ID |
| RESULT_ID | BIGINT | NO | FK | 작업실적ID |
| PROC_RESULT_ID | BIGINT | YES | FK | 공정실적ID |
| DEFECT_DT | DATE | NO | | 불량발생일 |
| DEFECT_TYPE | VARCHAR(20) | NO | | 불량유형 |
| DEFECT_CD | VARCHAR(20) | NO | | 불량코드 |
| DEFECT_QTY | DECIMAL(15,3) | NO | | 불량수량 |
| DEFECT_CAUSE | VARCHAR(500) | YES | | 불량원인 |
| DEFECT_ACTION | VARCHAR(500) | YES | | 조치내용 |
| DISPOSITION | VARCHAR(20) | YES | | 처리방법 (폐기/재작업/재검사/특채) |
| LOT_NO | VARCHAR(50) | YES | | LOT번호 |
| DEFECT_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

---

### 3.5 품질관리 테이블

#### QC_INSPECT_STD (검사기준)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| INSPECT_STD_ID | BIGINT | NO | PK,AI | 검사기준ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| ITEM_CD | VARCHAR(50) | NO | FK | 품목코드 |
| INSPECT_TYPE | VARCHAR(20) | NO | | 검사유형 (수입/공정/출하) |
| INSPECT_ITEM | VARCHAR(100) | NO | | 검사항목 |
| INSPECT_METHOD | VARCHAR(500) | YES | | 검사방법 |
| SPEC_VALUE | VARCHAR(100) | YES | | 규격값 |
| LSL | DECIMAL(15,5) | YES | | 하한규격 |
| USL | DECIMAL(15,5) | YES | | 상한규격 |
| TARGET | DECIMAL(15,5) | YES | | 목표값 |
| UNIT | VARCHAR(20) | YES | | 단위 |
| SAMPLE_SIZE | INT | YES | | 샘플크기 |
| SAMPLE_METHOD | VARCHAR(50) | YES | | 샘플링방법 |
| MANDATORY_YN | CHAR(1) | NO | | 필수여부 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### QC_INSPECT_RESULT (검사실적)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| INSPECT_ID | BIGINT | NO | PK,AI | 검사ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| INSPECT_NO | VARCHAR(30) | NO | UK | 검사번호 |
| INSPECT_TYPE | VARCHAR(20) | NO | | 검사유형 |
| INSPECT_DT | DATE | NO | | 검사일자 |
| ITEM_CD | VARCHAR(50) | NO | FK | 품목코드 |
| LOT_NO | VARCHAR(50) | YES | | LOT번호 |
| INSPECT_QTY | DECIMAL(15,3) | NO | | 검사수량 |
| PASS_QTY | DECIMAL(15,3) | YES | | 합격수량 |
| FAIL_QTY | DECIMAL(15,3) | YES | | 불합격수량 |
| INSPECTOR_ID | VARCHAR(20) | NO | FK | 검사자ID |
| WO_ID | BIGINT | YES | FK | 작업지시ID |
| RECEIVE_ID | BIGINT | YES | | 입고ID |
| JUDGE_RESULT | VARCHAR(20) | NO | | 판정결과 (합격/불합격/조건부합격) |
| JUDGE_DTM | DATETIME | YES | | 판정일시 |
| INSPECT_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### QC_INSPECT_DETAIL (검사상세)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| INSPECT_DTL_ID | BIGINT | NO | PK,AI | 검사상세ID |
| INSPECT_ID | BIGINT | NO | FK | 검사ID |
| INSPECT_STD_ID | BIGINT | NO | FK | 검사기준ID |
| INSPECT_ITEM | VARCHAR(100) | NO | | 검사항목 |
| MEASURE_VALUE | DECIMAL(15,5) | YES | | 측정값 |
| MEASURE_TEXT | VARCHAR(200) | YES | | 측정값(텍스트) |
| LSL | DECIMAL(15,5) | YES | | 하한규격 |
| USL | DECIMAL(15,5) | YES | | 상한규격 |
| JUDGE_RESULT | VARCHAR(20) | NO | | 판정결과 |
| DEFECT_CD | VARCHAR(20) | YES | | 불량코드 |
| DETAIL_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |

#### QC_DEFECT_HIS (품질불량이력)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| QC_DEFECT_ID | BIGINT | NO | PK,AI | 품질불량ID |
| INSPECT_ID | BIGINT | NO | FK | 검사ID |
| DEFECT_DT | DATE | NO | | 불량발생일 |
| DEFECT_CD | VARCHAR(20) | NO | | 불량코드 |
| DEFECT_QTY | DECIMAL(15,3) | NO | | 불량수량 |
| DEFECT_CAUSE | VARCHAR(500) | YES | | 불량원인 |
| CORRECTIVE_ACTION | VARCHAR(500) | YES | | 시정조치 |
| PREVENTIVE_ACTION | VARCHAR(500) | YES | | 예방조치 |
| DISPOSITION | VARCHAR(20) | YES | | 처리방법 |
| DISPOSITION_DT | DATE | YES | | 처리일자 |
| DEFECT_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### QC_DEFECT_CODE (불량코드)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| DEFECT_CD | VARCHAR(20) | NO | PK | 불량코드 |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| DEFECT_NM | VARCHAR(100) | NO | | 불량명 |
| DEFECT_TYPE | VARCHAR(20) | NO | | 불량유형 |
| DEFECT_DESC | VARCHAR(500) | YES | | 불량설명 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

---

### 3.6 재고관리 테이블

#### INV_STOCK (재고현황)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| STOCK_ID | BIGINT | NO | PK,AI | 재고ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| WAREHOUSE_CD | VARCHAR(20) | NO | FK | 창고코드 |
| LOCATION_CD | VARCHAR(50) | YES | | 로케이션코드 |
| ITEM_CD | VARCHAR(50) | NO | FK | 품목코드 |
| LOT_NO | VARCHAR(50) | YES | | LOT번호 |
| STOCK_QTY | DECIMAL(15,3) | NO | | 재고수량 |
| RESERVED_QTY | DECIMAL(15,3) | YES | | 예약수량 |
| AVAILABLE_QTY | DECIMAL(15,3) | YES | | 가용수량 |
| UNIT | VARCHAR(20) | NO | | 단위 |
| STOCK_STATUS | VARCHAR(20) | NO | | 재고상태 (정상/보류/불량) |
| LAST_TRANS_DT | DATE | YES | | 최종거래일 |
| EXPIRE_DT | DATE | YES | | 유효기한 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### INV_TRANS_HIS (입출고이력)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| TRANS_ID | BIGINT | NO | PK,AI | 거래ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| TRANS_NO | VARCHAR(30) | NO | UK | 거래번호 |
| TRANS_DT | DATE | NO | | 거래일자 |
| TRANS_TYPE | VARCHAR(20) | NO | | 거래유형 (입고/출고/이동/조정) |
| TRANS_REASON | VARCHAR(50) | NO | | 거래사유 (구매입고/생산입고/판매출고/생산출고 등) |
| ITEM_CD | VARCHAR(50) | NO | FK | 품목코드 |
| LOT_NO | VARCHAR(50) | YES | | LOT번호 |
| TRANS_QTY | DECIMAL(15,3) | NO | | 거래수량 |
| UNIT | VARCHAR(20) | NO | | 단위 |
| FROM_WAREHOUSE_CD | VARCHAR(20) | YES | FK | 출고창고코드 |
| TO_WAREHOUSE_CD | VARCHAR(20) | YES | FK | 입고창고코드 |
| FROM_LOCATION_CD | VARCHAR(50) | YES | | 출고로케이션 |
| TO_LOCATION_CD | VARCHAR(50) | YES | | 입고로케이션 |
| REF_TYPE | VARCHAR(20) | YES | | 참조유형 (작업지시/발주/수주 등) |
| REF_NO | VARCHAR(50) | YES | | 참조번호 |
| VENDOR_CD | VARCHAR(50) | YES | FK | 거래처코드 |
| UNIT_PRICE | DECIMAL(18,2) | YES | | 단가 |
| TRANS_AMT | DECIMAL(18,2) | YES | | 거래금액 |
| TRANS_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### INV_LOT (LOT정보)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| LOT_ID | BIGINT | NO | PK,AI | LOT ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| LOT_NO | VARCHAR(50) | NO | UK | LOT번호 |
| ITEM_CD | VARCHAR(50) | NO | FK | 품목코드 |
| LOT_QTY | DECIMAL(15,3) | NO | | LOT수량 |
| CREATE_DT | DATE | NO | | 생성일 |
| EXPIRE_DT | DATE | YES | | 유효기한 |
| VENDOR_CD | VARCHAR(50) | YES | FK | 공급업체 |
| VENDOR_LOT_NO | VARCHAR(50) | YES | | 공급업체LOT |
| PARENT_LOT_NO | VARCHAR(50) | YES | | 상위LOT번호 |
| WO_ID | BIGINT | YES | FK | 작업지시ID |
| LOT_STATUS | VARCHAR(20) | NO | | LOT상태 (정상/보류/폐기) |
| LOT_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### INV_LOT_TRACE (LOT추적이력)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| TRACE_ID | BIGINT | NO | PK,AI | 추적ID |
| LOT_NO | VARCHAR(50) | NO | FK | LOT번호 |
| PARENT_LOT_NO | VARCHAR(50) | YES | | 상위LOT번호 |
| CHILD_LOT_NO | VARCHAR(50) | YES | | 하위LOT번호 |
| TRACE_TYPE | VARCHAR(20) | NO | | 추적유형 (정방향/역방향) |
| PROCESS_CD | VARCHAR(20) | YES | | 공정코드 |
| WO_ID | BIGINT | YES | FK | 작업지시ID |
| TRACE_DTM | DATETIME | NO | | 추적일시 |
| TRACE_QTY | DECIMAL(15,3) | YES | | 추적수량 |
| TRACE_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |

---

### 3.7 설비관리 테이블

#### EQP_OPER_STATUS (설비가동현황)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| OPER_ID | BIGINT | NO | PK,AI | 가동ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| EQUIPMENT_CD | VARCHAR(20) | NO | FK | 설비코드 |
| OPER_DT | DATE | NO | | 가동일자 |
| SHIFT | VARCHAR(10) | NO | | 근무조 |
| OPER_STATUS | VARCHAR(20) | NO | | 가동상태 (가동/비가동/대기) |
| START_DTM | DATETIME | NO | | 시작일시 |
| END_DTM | DATETIME | YES | | 종료일시 |
| OPER_TIME | DECIMAL(10,2) | YES | | 가동시간(분) |
| WO_ID | BIGINT | YES | FK | 작업지시ID |
| OPER_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### EQP_DOWNTIME (비가동이력)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| DOWNTIME_ID | BIGINT | NO | PK,AI | 비가동ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| EQUIPMENT_CD | VARCHAR(20) | NO | FK | 설비코드 |
| DOWNTIME_DT | DATE | NO | | 비가동일자 |
| DOWNTIME_TYPE | VARCHAR(20) | NO | | 비가동유형 (고장/정비/대기/기타) |
| DOWNTIME_CD | VARCHAR(20) | NO | | 비가동코드 |
| DOWNTIME_REASON | VARCHAR(500) | YES | | 비가동사유 |
| START_DTM | DATETIME | NO | | 시작일시 |
| END_DTM | DATETIME | YES | | 종료일시 |
| DOWNTIME_MIN | DECIMAL(10,2) | YES | | 비가동시간(분) |
| ACTION_CONTENT | VARCHAR(500) | YES | | 조치내용 |
| REPORTER_ID | VARCHAR(20) | YES | FK | 보고자ID |
| DOWNTIME_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### EQP_MAINT_HIS (설비정비이력)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| MAINT_ID | BIGINT | NO | PK,AI | 정비ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| EQUIPMENT_CD | VARCHAR(20) | NO | FK | 설비코드 |
| MAINT_NO | VARCHAR(30) | NO | UK | 정비번호 |
| MAINT_TYPE | VARCHAR(20) | NO | | 정비유형 (예방정비/사후정비/개량정비) |
| MAINT_DT | DATE | NO | | 정비일자 |
| START_DTM | DATETIME | NO | | 시작일시 |
| END_DTM | DATETIME | YES | | 종료일시 |
| MAINT_TIME | DECIMAL(10,2) | YES | | 정비시간(분) |
| MAINT_WORKER_ID | VARCHAR(20) | YES | FK | 정비자ID |
| MAINT_CONTENT | VARCHAR(1000) | YES | | 정비내용 |
| PART_REPLACED | VARCHAR(500) | YES | | 교체부품 |
| MAINT_COST | DECIMAL(18,2) | YES | | 정비비용 |
| MAINT_RESULT | VARCHAR(20) | NO | | 정비결과 (완료/진행중/보류) |
| NEXT_MAINT_DT | DATE | YES | | 다음정비예정일 |
| MAINT_RMK | VARCHAR(500) | YES | | 비고 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

#### EQP_MAINT_PLAN (정비계획)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| MAINT_PLAN_ID | BIGINT | NO | PK,AI | 정비계획ID |
| PLANT_CD | VARCHAR(20) | NO | FK | 공장코드 |
| EQUIPMENT_CD | VARCHAR(20) | NO | FK | 설비코드 |
| MAINT_PLAN_NM | VARCHAR(100) | NO | | 정비계획명 |
| MAINT_TYPE | VARCHAR(20) | NO | | 정비유형 |
| CYCLE_TYPE | VARCHAR(20) | NO | | 주기유형 (일/주/월/분기/년) |
| CYCLE_VALUE | INT | NO | | 주기값 |
| LAST_MAINT_DT | DATE | YES | | 최종정비일 |
| NEXT_MAINT_DT | DATE | YES | | 다음정비예정일 |
| MAINT_CHECKLIST | TEXT | YES | | 정비체크리스트 |
| USE_YN | CHAR(1) | NO | | 사용여부 |
| REG_USER_ID | VARCHAR(50) | NO | | 등록자ID |
| REG_DTM | DATETIME | NO | | 등록일시 |
| UPD_USER_ID | VARCHAR(50) | YES | | 수정자ID |
| UPD_DTM | DATETIME | YES | | 수정일시 |

---

## 4. 인덱스 설계

### 4.1 주요 인덱스 목록

```sql
-- 기준정보
CREATE INDEX IDX_MST_ITEM_01 ON MST_ITEM(PLANT_CD, ITEM_TYPE);
CREATE INDEX IDX_MST_ITEM_02 ON MST_ITEM(ITEM_NM);
CREATE INDEX IDX_MST_BOM_01 ON MST_BOM(PARENT_ITEM_CD);
CREATE INDEX IDX_MST_BOM_02 ON MST_BOM(CHILD_ITEM_CD);
CREATE INDEX IDX_MST_ROUTING_01 ON MST_ROUTING(ITEM_CD, PROCESS_SEQ);

-- 생산계획
CREATE INDEX IDX_PLN_WORK_ORDER_01 ON PLN_WORK_ORDER(PLANT_CD, WO_DT, WO_STATUS);
CREATE INDEX IDX_PLN_WORK_ORDER_02 ON PLN_WORK_ORDER(ITEM_CD);
CREATE INDEX IDX_PLN_WORK_ORDER_03 ON PLN_WORK_ORDER(LOT_NO);

-- 생산실적
CREATE INDEX IDX_PRD_WORK_RESULT_01 ON PRD_WORK_RESULT(WO_ID);
CREATE INDEX IDX_PRD_WORK_RESULT_02 ON PRD_WORK_RESULT(PLANT_CD, RESULT_DT);
CREATE INDEX IDX_PRD_WORK_RESULT_03 ON PRD_WORK_RESULT(ITEM_CD);
CREATE INDEX IDX_PRD_PROCESS_RESULT_01 ON PRD_PROCESS_RESULT(RESULT_ID);

-- 품질관리
CREATE INDEX IDX_QC_INSPECT_RESULT_01 ON QC_INSPECT_RESULT(PLANT_CD, INSPECT_DT);
CREATE INDEX IDX_QC_INSPECT_RESULT_02 ON QC_INSPECT_RESULT(ITEM_CD, LOT_NO);

-- 재고관리
CREATE INDEX IDX_INV_STOCK_01 ON INV_STOCK(PLANT_CD, WAREHOUSE_CD, ITEM_CD);
CREATE INDEX IDX_INV_STOCK_02 ON INV_STOCK(ITEM_CD, LOT_NO);
CREATE INDEX IDX_INV_TRANS_HIS_01 ON INV_TRANS_HIS(PLANT_CD, TRANS_DT);
CREATE INDEX IDX_INV_TRANS_HIS_02 ON INV_TRANS_HIS(ITEM_CD);
CREATE INDEX IDX_INV_LOT_01 ON INV_LOT(ITEM_CD);

-- 설비관리
CREATE INDEX IDX_EQP_OPER_STATUS_01 ON EQP_OPER_STATUS(EQUIPMENT_CD, OPER_DT);
CREATE INDEX IDX_EQP_DOWNTIME_01 ON EQP_DOWNTIME(EQUIPMENT_CD, DOWNTIME_DT);
CREATE INDEX IDX_EQP_MAINT_HIS_01 ON EQP_MAINT_HIS(EQUIPMENT_CD, MAINT_DT);
```

---

## 5. 데이터 관계 (Foreign Key)

### 5.1 주요 FK 관계

| 테이블 | FK 컬럼 | 참조 테이블 | 참조 컬럼 |
|--------|---------|-------------|-----------|
| MST_PLANT | COMPANY_CD | MST_COMPANY | COMPANY_CD |
| MST_DEPT | PLANT_CD | MST_PLANT | PLANT_CD |
| MST_WORKER | PLANT_CD | MST_PLANT | PLANT_CD |
| MST_WORKER | DEPT_CD | MST_DEPT | DEPT_CD |
| MST_WORKCENTER | PLANT_CD | MST_PLANT | PLANT_CD |
| MST_ITEM | PLANT_CD | MST_PLANT | PLANT_CD |
| MST_BOM | PARENT_ITEM_CD | MST_ITEM | ITEM_CD |
| MST_BOM | CHILD_ITEM_CD | MST_ITEM | ITEM_CD |
| MST_ROUTING | ITEM_CD | MST_ITEM | ITEM_CD |
| MST_ROUTING | WORKCENTER_CD | MST_WORKCENTER | WORKCENTER_CD |
| MST_EQUIPMENT | WORKCENTER_CD | MST_WORKCENTER | WORKCENTER_CD |
| PLN_WORK_ORDER | PLAN_ID | PLN_PROD_PLAN | PLAN_ID |
| PLN_WORK_ORDER | ITEM_CD | MST_ITEM | ITEM_CD |
| PRD_WORK_RESULT | WO_ID | PLN_WORK_ORDER | WO_ID |
| PRD_WORK_RESULT | WORKER_ID | MST_WORKER | WORKER_ID |
| QC_INSPECT_RESULT | ITEM_CD | MST_ITEM | ITEM_CD |
| INV_STOCK | WAREHOUSE_CD | MST_WAREHOUSE | WAREHOUSE_CD |
| INV_STOCK | ITEM_CD | MST_ITEM | ITEM_CD |
| EQP_OPER_STATUS | EQUIPMENT_CD | MST_EQUIPMENT | EQUIPMENT_CD |

---

## 6. 주요 뷰 (View) 설계

### 6.1 생산현황 모니터링 뷰

```sql
-- 일별 생산실적 현황
CREATE OR REPLACE VIEW V_DAILY_PROD_SUMMARY AS
SELECT 
    r.PLANT_CD,
    r.RESULT_DT,
    r.ITEM_CD,
    i.ITEM_NM,
    COUNT(DISTINCT r.WO_ID) AS WO_CNT,
    SUM(r.PROD_QTY) AS TOTAL_PROD_QTY,
    SUM(r.GOOD_QTY) AS TOTAL_GOOD_QTY,
    SUM(r.DEFECT_QTY) AS TOTAL_DEFECT_QTY,
    ROUND(SUM(r.GOOD_QTY) / NULLIF(SUM(r.PROD_QTY), 0) * 100, 2) AS YIELD_RATE
FROM PRD_WORK_RESULT r
JOIN MST_ITEM i ON r.ITEM_CD = i.ITEM_CD
GROUP BY r.PLANT_CD, r.RESULT_DT, r.ITEM_CD, i.ITEM_NM;
```

### 6.2 재고현황 뷰

```sql
-- 품목별 재고 현황
CREATE OR REPLACE VIEW V_STOCK_SUMMARY AS
SELECT 
    s.PLANT_CD,
    s.ITEM_CD,
    i.ITEM_NM,
    i.ITEM_TYPE,
    SUM(s.STOCK_QTY) AS TOTAL_STOCK_QTY,
    SUM(s.RESERVED_QTY) AS TOTAL_RESERVED_QTY,
    SUM(s.AVAILABLE_QTY) AS TOTAL_AVAILABLE_QTY,
    i.SAFETY_STOCK_QTY,
    CASE WHEN SUM(s.AVAILABLE_QTY) < COALESCE(i.SAFETY_STOCK_QTY, 0) 
         THEN 'Y' ELSE 'N' END AS LOW_STOCK_YN
FROM INV_STOCK s
JOIN MST_ITEM i ON s.ITEM_CD = i.ITEM_CD
WHERE s.STOCK_STATUS = '정상'
GROUP BY s.PLANT_CD, s.ITEM_CD, i.ITEM_NM, i.ITEM_TYPE, i.SAFETY_STOCK_QTY;
```

### 6.3 설비 가동률 뷰

```sql
-- 설비별 일간 가동률
CREATE OR REPLACE VIEW V_EQUIPMENT_UTILIZATION AS
SELECT 
    o.PLANT_CD,
    o.EQUIPMENT_CD,
    e.EQUIPMENT_NM,
    o.OPER_DT,
    SUM(CASE WHEN o.OPER_STATUS = '가동' THEN o.OPER_TIME ELSE 0 END) AS RUN_TIME,
    SUM(CASE WHEN o.OPER_STATUS = '비가동' THEN o.OPER_TIME ELSE 0 END) AS DOWN_TIME,
    SUM(o.OPER_TIME) AS TOTAL_TIME,
    ROUND(SUM(CASE WHEN o.OPER_STATUS = '가동' THEN o.OPER_TIME ELSE 0 END) 
          / NULLIF(SUM(o.OPER_TIME), 0) * 100, 2) AS UTILIZATION_RATE
FROM EQP_OPER_STATUS o
JOIN MST_EQUIPMENT e ON o.EQUIPMENT_CD = e.EQUIPMENT_CD
GROUP BY o.PLANT_CD, o.EQUIPMENT_CD, e.EQUIPMENT_NM, o.OPER_DT;
```

---

## 7. 번호 채번 규칙

### 7.1 자동 채번 테이블

#### COM_SEQ_NO (채번관리)
| 컬럼명 | 데이터타입 | NULL | 키 | 설명 |
|--------|-----------|------|-----|------|
| SEQ_TYPE | VARCHAR(20) | NO | PK | 채번유형 |
| PREFIX | VARCHAR(10) | NO | | 접두어 |
| SEQ_DT | DATE | NO | PK | 채번일자 |
| CURRENT_SEQ | INT | NO | | 현재순번 |
| SEQ_LENGTH | INT | NO | | 순번자릿수 |

### 7.2 번호 체계

| 구분 | 형식 | 예시 |
|------|------|------|
| 작업지시번호 | WO + YYYYMMDD + 4자리순번 | WO202401150001 |
| 생산계획번호 | PP + YYYYMMDD + 4자리순번 | PP202401150001 |
| 검사번호 | QC + YYYYMMDD + 4자리순번 | QC202401150001 |
| 입출고번호 | TR + YYYYMMDD + 4자리순번 | TR202401150001 |
| LOT번호 | 품목코드 + YYYYMMDD + 3자리순번 | A001-20240115-001 |
| 정비번호 | MT + YYYYMMDD + 4자리순번 | MT202401150001 |

---

## 8. 테이블 요약

| 영역 | 테이블 수 | 주요 테이블 |
|------|----------|-------------|
| 공통코드 | 2 | COM_CODE_GRP, COM_CODE |
| 기준정보 | 10 | MST_COMPANY, MST_PLANT, MST_ITEM, MST_BOM 등 |
| 생산계획 | 2 | PLN_PROD_PLAN, PLN_WORK_ORDER |
| 생산실적 | 3 | PRD_WORK_RESULT, PRD_PROCESS_RESULT, PRD_DEFECT_HIS |
| 품질관리 | 5 | QC_INSPECT_STD, QC_INSPECT_RESULT 등 |
| 재고관리 | 4 | INV_STOCK, INV_TRANS_HIS, INV_LOT 등 |
| 설비관리 | 4 | EQP_OPER_STATUS, EQP_DOWNTIME 등 |
| **합계** | **30** | |

---

## 9. 다음 단계

1. **DDL 스크립트 생성**: 위 설계를 바탕으로 MariaDB용 CREATE TABLE 스크립트 작성
2. **초기 데이터 구성**: 공통코드, 기준정보 초기 데이터 입력
3. **저장 프로시저 개발**: 주요 업무 로직용 SP 개발
4. **인터페이스 설계**: ERP, SCM 등 외부 시스템 연계 인터페이스 정의
