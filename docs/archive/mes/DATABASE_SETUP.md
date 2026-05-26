# MES Backend Server - 데이터베이스 셋업 가이드

## 개요
이 문서는 `mes_backend_server` 데이터베이스를 다른 컴퓨터에 복원하는 방법을 설명합니다.

---

## 필수 요구사항

- Docker & Docker Compose 설치
- MariaDB 또는 MySQL 운영 중

---

## 데이터베이스 복원 방법

### 방법 1: Docker를 사용하는 경우 (권장)

#### 1. MariaDB 컨테이너 실행
```bash
docker run -d \
  --name mariadb \
  -e MYSQL_ROOT_PASSWORD=551654 \
  -p 3306:3306 \
  mariadb:latest
```

#### 2. 덤프 파일 복원
```bash
# 덤프 파일 경로 설정
DUMP_FILE="./mes_backend_server_backup.sql"

# 덤프 파일 복원
docker exec -i mariadb mariadb -u root -p551654 < $DUMP_FILE
```

#### 3. 복원 확인
```bash
docker exec mariadb mariadb -u root -p551654 -e "SHOW DATABASES LIKE 'mes_backend_server';"
```

---

### 방법 2: 로컬 MariaDB/MySQL을 사용하는 경우

#### 1. 덤프 파일 복원
```bash
# Linux/Mac
mariadb -u root -p < mes_backend_server_backup.sql

# 또는
mysql -u root -p < mes_backend_server_backup.sql
```

#### 2. 복원 확인
```bash
mariadb -u root -p -e "SHOW DATABASES LIKE 'mes_backend_server';"
```

---

### 방법 3: Docker Compose를 사용하는 경우

#### 1. docker-compose.yml 파일 생성
```yaml
version: '3.8'

services:
  mariadb:
    image: mariadb:latest
    container_name: mariadb
    environment:
      MYSQL_ROOT_PASSWORD: 551654
    ports:
      - "3306:3306"
    volumes:
      - mariadb_data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci

volumes:
  mariadb_data:
```

#### 2. 컨테이너 시작
```bash
docker-compose up -d
```

#### 3. 덤프 파일 복원
```bash
docker-compose exec -T mariadb mariadb -u root -p551654 < mes_backend_server_backup.sql
```

#### 4. 복원 확인
```bash
docker-compose exec mariadb mariadb -u root -p551654 -e "SHOW DATABASES LIKE 'mes_backend_server';"
```

---

## 데이터베이스 연결 정보

- **호스트**: localhost
- **포트**: 3306
- **사용자명**: root
- **비밀번호**: 551654
- **데이터베이스**: mes_backend_server
- **문자 집합**: utf8mb4

---

## 데이터베이스 구조

### 기준정보 (Master Data)
- MST_COMPANY: 회사정보 (1개)
- MST_PLANT: 공장정보 (2개)
- MST_DEPT: 부서정보 (13개)
- MST_WORKER: 작업자정보 (20개)
- MST_WORKCENTER: 작업장정보 (10개)
- MST_EQUIPMENT: 설비정보 (24개)
- MST_VENDOR: 거래처정보 (14개)
- MST_WAREHOUSE: 창고정보 (9개)
- MST_ITEM: 품목정보 (55개)
- MST_BOM: BOM정보 (12개)
- MST_ROUTING: 라우팅정보 (16개)

### 생산 계획/실적
- PLN_PROD_PLAN: 생산계획 (5개)
- PLN_WORK_ORDER: 작업지시 (7개)
- PRD_WORK_RESULT: 작업실적 (7개)

### 품질관리
- QC_DEFECT_CODE: 불량코드 (22개)
- QC_INSPECT_STD: 검사기준 (5개)
- QC_INSPECT_RESULT: 검사실적 (4개)

### 재고관리
- INV_LOT: LOT정보 (9개)
- INV_STOCK: 재고현황 (9개)

### 설비관리
- EQP_DOWNTIME_CODE: 비가동코드 (8개)
- EQP_DOWNTIME: 비가동이력 (4개)
- EQP_OPER_STATUS: 설비가동현황 (4개)

**총 59개 테이블, 약 243개 데이터**

---

## 연결 테스트

### Node.js/JavaScript
```javascript
const mysql = require('mysql2/promise');

const connection = await mysql.createConnection({
  host: 'localhost',
  user: 'root',
  password: '551654',
  database: 'mes_backend_server'
});

console.log('Connected!');
```

### Python
```python
import mysql.connector

conn = mysql.connector.connect(
  host="localhost",
  user="root",
  password="551654",
  database="mes_backend_server"
)

cursor = conn.cursor()
cursor.execute("SELECT COUNT(*) FROM MST_COMPANY")
print(cursor.fetchone())
```

### Java
```java
String url = "jdbc:mysql://localhost:3306/mes_backend_server";
String user = "root";
String password = "551654";

Connection conn = DriverManager.getConnection(url, user, password);
System.out.println("Connected!");
```

---

## 백업 생성

### 새로운 덤프 파일 생성
```bash
# Docker 사용 시
docker exec mariadb mariadb-dump -u root -p551654 mes_backend_server > backup_$(date +%Y%m%d_%H%M%S).sql

# 로컬 MariaDB 사용 시
mariadb-dump -u root -p mes_backend_server > backup_$(date +%Y%m%d_%H%M%S).sql
```

---

## 문제 해결

### 1. 덤프 파일 복원 중 권한 오류
```bash
# Docker에서 파일 소유권 확인
docker exec mariadb ls -l /tmp/mes_backend_server_backup.sql
```

### 2. 문자 인코딩 문제
덤프 파일이 이미 `utf8mb4`로 설정되어 있으므로 특별한 설정이 필요 없습니다.

### 3. 테이블이 보이지 않는 경우
```bash
# 테이블 목록 확인
docker exec mariadb mariadb -u root -p551654 mes_backend_server -e "SHOW TABLES;"

# 데이터 건수 확인
docker exec mariadb mariadb -u root -p551654 mes_backend_server -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='mes_backend_server';"
```

---

## 참고 사항

- 덤프 파일은 전체 데이터베이스 스키마 및 데이터를 포함합니다.
- 덤프 파일 크기: ~193KB
- 복원 시간: 10~30초 (시스템 성능에 따라 다름)
- 기본 설정된 비밀번호 `551654`는 보안상 프로덕션 환경에서 변경이 필요합니다.

---

## 추가 정보

- **생성 날짜**: 2025-01-22
- **MariaDB 버전**: 11.7.2
- **문자 집합**: utf8mb4
- **콜레이션**: utf8mb4_general_ci
