-- ============================================================
-- 博物馆参观预约系统 数据库初始化脚本
-- MySQL 8.0+
-- 默认演示账号：
--   admin   / Admin@123    系统管理员
--   manager / Manager@123  预约管理员
--   visitor / Visitor@123  游客
-- ============================================================

CREATE DATABASE IF NOT EXISTS museum_reservation
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE museum_reservation;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS checkin_record;
DROP TABLE IF EXISTS payment_order;
DROP TABLE IF EXISTS slot_guest_lock;
DROP TABLE IF EXISTS visitor;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS visit_slot;
DROP TABLE IF EXISTS ticket_type;
DROP TABLE IF EXISTS exhibition;
DROP TABLE IF EXISTS museum;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS system_config;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    real_name       VARCHAR(50)  NOT NULL,
    phone           VARCHAR(20),
    id_card         VARCHAR(18),
    email           VARCHAR(100),
    avatar          VARCHAR(500),
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    last_login_time DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE sys_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(50) NOT NULL,
    role_name   VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

CREATE TABLE sys_user_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_ur_role (role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表';

CREATE TABLE museum (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    description TEXT,
    open_time   VARCHAR(50)  DEFAULT '09:00',
    close_time  VARCHAR(50)  DEFAULT '17:00',
    phone       VARCHAR(50),
    cover_url   VARCHAR(500),
    status      TINYINT      NOT NULL DEFAULT 1,
    sort        INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_museum_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '博物馆表';

CREATE TABLE exhibition (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    museum_id   BIGINT       NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    cover_url   VARCHAR(500),
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_exhibition_museum (museum_id),
    KEY idx_exhibition_date (start_date, end_date),
    CONSTRAINT fk_exhibition_museum FOREIGN KEY (museum_id) REFERENCES museum (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '展览表';

CREATE TABLE ticket_type (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    museum_id    BIGINT        NOT NULL,
    name         VARCHAR(80)   NOT NULL,
    price        DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ticket_kind  VARCHAR(20)   NOT NULL DEFAULT 'BASIC' COMMENT 'BASIC/SINGLE/COMBO',
    exhibition_id BIGINT,
    audience_type VARCHAR(20)  NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL/ADULT/STUDENT/FAMILY',
    tagline      VARCHAR(40)   NOT NULL DEFAULT '',
    audience     VARCHAR(150),
    description  VARCHAR(255),
    benefits     VARCHAR(800),
    rules        VARCHAR(800),
    status       TINYINT       NOT NULL DEFAULT 1,
    sort         INT           NOT NULL DEFAULT 0,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ticket_museum (museum_id),
    KEY idx_ticket_exhibition (exhibition_id),
    CONSTRAINT fk_ticket_museum FOREIGN KEY (museum_id) REFERENCES museum (id),
    CONSTRAINT fk_ticket_exhibition FOREIGN KEY (exhibition_id) REFERENCES exhibition (id),
    CONSTRAINT ck_ticket_price CHECK (price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '票种表';

CREATE TABLE visit_slot (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    museum_id      BIGINT NOT NULL,
    exhibition_id  BIGINT,
    visit_date     DATE   NOT NULL,
    start_time     TIME   NOT NULL,
    end_time       TIME   NOT NULL,
    total_capacity INT    NOT NULL DEFAULT 200,
    booked_count   INT    NOT NULL DEFAULT 0,
    status         TINYINT NOT NULL DEFAULT 1,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_slot_museum_date_time (museum_id, visit_date, start_time),
    KEY idx_slot_date (visit_date),
    KEY idx_slot_museum (museum_id),
    KEY idx_slot_exhibition (exhibition_id),
    CONSTRAINT fk_slot_museum FOREIGN KEY (museum_id) REFERENCES museum (id),
    CONSTRAINT fk_slot_exhibition FOREIGN KEY (exhibition_id) REFERENCES exhibition (id),
    CONSTRAINT ck_slot_capacity CHECK (total_capacity >= 0),
    CONSTRAINT ck_slot_booked CHECK (booked_count >= 0 AND booked_count <= total_capacity),
    CONSTRAINT ck_slot_time CHECK (end_time > start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开放时段与票额';

CREATE TABLE reservation (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(40)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    museum_id       BIGINT        NOT NULL,
    exhibition_id   BIGINT,
    slot_id         BIGINT        NOT NULL,
    ticket_type_id  BIGINT        NOT NULL,
    visitor_count   INT           NOT NULL,
    unit_price      DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    contact_name    VARCHAR(50)   NOT NULL,
    contact_phone   VARCHAR(20)   NOT NULL,
    status          VARCHAR(20)   NOT NULL COMMENT 'PENDING_PAYMENT/RESERVED/CANCELED/CHECKED_IN/EXPIRED',
    qr_token        VARCHAR(255),
    qr_expire_at    DATETIME,
    remark          VARCHAR(500),
    cancel_time     DATETIME,
    checkin_time    DATETIME,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reservation_order (order_no),
    KEY idx_reservation_user (user_id, status),
    KEY idx_reservation_slot (slot_id, status),
    KEY idx_reservation_created (created_at),
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_reservation_museum FOREIGN KEY (museum_id) REFERENCES museum (id),
    CONSTRAINT fk_reservation_exhibition FOREIGN KEY (exhibition_id) REFERENCES exhibition (id),
    CONSTRAINT fk_reservation_slot FOREIGN KEY (slot_id) REFERENCES visit_slot (id),
    CONSTRAINT fk_reservation_ticket FOREIGN KEY (ticket_type_id) REFERENCES ticket_type (id),
    CONSTRAINT ck_reservation_status CHECK (status IN ('PENDING_PAYMENT', 'RESERVED', 'CANCELED', 'CHECKED_IN', 'EXPIRED')),
    CONSTRAINT ck_reservation_count CHECK (visitor_count >= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预约订单';

CREATE TABLE visitor (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id BIGINT      NOT NULL,
    slot_id        BIGINT      NOT NULL,
    name           VARCHAR(50) NOT NULL,
    id_card        VARCHAR(18) NOT NULL,
    phone          VARCHAR(20),
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_visitor_reservation (reservation_id),
    KEY idx_visitor_slot_card (slot_id, id_card),
    CONSTRAINT fk_visitor_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT fk_visitor_slot FOREIGN KEY (slot_id) REFERENCES visit_slot (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '参观者';

CREATE TABLE slot_guest_lock (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    slot_id        BIGINT      NOT NULL,
    id_card        VARCHAR(18) NOT NULL,
    reservation_id BIGINT,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_slot_guest (slot_id, id_card),
    KEY idx_guest_lock_reservation (reservation_id),
    CONSTRAINT fk_guest_lock_slot FOREIGN KEY (slot_id) REFERENCES visit_slot (id),
    CONSTRAINT fk_guest_lock_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '时段实名占位锁，防重复预约';

CREATE TABLE payment_order (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_no     VARCHAR(40)   NOT NULL,
    order_no       VARCHAR(40)   NOT NULL,
    reservation_id BIGINT        NOT NULL,
    user_id        BIGINT        NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    pay_type       VARCHAR(20)   NOT NULL DEFAULT 'MOCK',
    status         VARCHAR(20)   NOT NULL COMMENT 'UNPAID/PAID/REFUNDED/CLOSED',
    paid_time      DATETIME,
    refund_time    DATETIME,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_payment_reservation (reservation_id),
    KEY idx_payment_order (order_no),
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT ck_payment_status CHECK (status IN ('UNPAID', 'PAID', 'REFUNDED', 'CLOSED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付单（模拟支付）';

CREATE TABLE checkin_record (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id BIGINT      NOT NULL,
    order_no       VARCHAR(40) NOT NULL,
    checkin_time   DATETIME    NOT NULL,
    gate           VARCHAR(50),
    operator_id    BIGINT,
    operator_name  VARCHAR(50),
    status         VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_checkin_reservation (reservation_id),
    KEY idx_checkin_time (checkin_time),
    CONSTRAINT fk_checkin_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT fk_checkin_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '核销记录';

CREATE TABLE notice (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(200) NOT NULL,
    summary      VARCHAR(500),
    content      TEXT,
    cover_url    VARCHAR(500),
    status       TINYINT      NOT NULL DEFAULT 1,
    publish_time DATETIME,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_notice_status (status, publish_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公告';

CREATE TABLE operation_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT,
    username    VARCHAR(50),
    module      VARCHAR(50),
    operation   VARCHAR(100),
    method      VARCHAR(200),
    params      TEXT,
    ip          VARCHAR(64),
    duration_ms BIGINT,
    success     TINYINT NOT NULL DEFAULT 1,
    error_msg   VARCHAR(500),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_log_user (user_id),
    KEY idx_log_time (create_time),
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志';

CREATE TABLE system_config (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key   VARCHAR(80)  NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    description  VARCHAR(200),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统参数';

-- ============================================================
-- 基础数据
-- ============================================================
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'SYSTEM_ADMIN', '系统管理员', '用户、角色、日志、参数配置与全部业务权限'),
(2, 'RESERVATION_ADMIN', '预约管理员', '博物馆、展览、时段、票额、订单、核销与统计'),
(3, 'VISITOR', '游客', '浏览、预约、取消与电子票');

INSERT INTO sys_user (id, username, password_hash, real_name, phone, id_card, email, status) VALUES
(1, 'admin', '$2a$10$C7uoxhzWaU4eaom6PsnGvONgq00vJFmfAubtq3scCq/fA3Gc2k/uu', '系统管理员', '13800000001', '110101199001010011', 'admin@museum.demo', 1),
(2, 'manager', '$2a$10$K0eVdgYXLb0DGviI32Yn3O4w55fgq5cZuJ.r/HT9Sx/JvAmQxDht2', '预约管理员', '13800000002', '110101199202020022', 'manager@museum.demo', 1),
(3, 'visitor', '$2a$10$ioHB36.fG6Hw2VLQS6qCrOjd9SH3VqIUeeHlMcIHPkW42VtiHwJAK', '林小满', '13900000003', '110101199505050033', 'visitor@museum.demo', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3);

UPDATE sys_user SET created_at = DATE_SUB(CURDATE(), INTERVAL 7 DAY),
                    updated_at = DATE_SUB(CURDATE(), INTERVAL 7 DAY);

INSERT INTO museum (id, name, address, description, open_time, close_time, phone, cover_url, status, sort) VALUES
(1, '故宫博物院', '北京市东城区景山前街4号',
 '红墙黄瓦之间，六百年宫城在光影中徐徐展开。故宫博物院以明清宫廷建筑、书画、陶瓷与宫廷器物收藏闻名，是理解中华文明连续性的一扇窗口。参观请提前预约，并按预约时段由午门入院。',
 '08:30', '17:00', '010-85007938',
 '/images/seed/museum-1.jpg', 1, 100),
(2, '上海博物馆', '上海市黄浦区人民大道201号',
 '上海博物馆以中国古代青铜器、陶瓷、书法与绘画收藏著称。建筑取“天圆地方”之意，常设展与特展并行，在人民广场的喧嚣中提供一处安静而深远的观看空间。',
 '09:00', '17:00', '021-63723500',
 '/images/seed/museum-2.jpg', 1, 90),
(3, '苏州博物馆', '江苏省苏州市姑苏区东北街204号',
 '粉墙黛瓦、片石假山，贝聿铭以现代几何语言回应苏州园林的传统。苏州博物馆将吴地文人书画、工艺与建筑空间融为一体，是江南文化的一次当代表达。',
 '09:00', '17:00', '0512-67575666',
 '/images/seed/museum-3.jpg', 1, 80);

INSERT INTO exhibition (id, museum_id, name, description, cover_url, start_date, end_date, status) VALUES
(1, 1, '千里江山：宋代绘画特展',
 '以《千里江山图》为线索，汇集宋代山水、花鸟与人物画精品，呈现宋人观看自然的方式。',
 '/images/seed/exhibition-1.jpg',
 DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 40 DAY), 1),
(2, 1, '宫城记忆：明清宫廷生活展',
 '从朝仪、服饰到日常器用，重新讲述宫城之中的人与生活。',
 '/images/seed/exhibition-2.jpg',
 DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 1),
(3, 2, '青铜之光：商周礼器特展',
 '以青铜礼器为核心，解读器形、铭文与礼制背后的早期中国。',
 '/images/seed/exhibition-3.jpg',
 DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 50 DAY), 1),
(4, 2, '海上风华：近代上海设计展',
 '从月份牌、家具到城市空间，回望近代上海的摩登经验。',
 '/images/seed/exhibition-4.jpg',
 DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY), 1),
(5, 3, '吴门四家：文人书画特展',
 '沈周、文徵明、唐寅、仇英，四人笔下的江南与日常生活。',
 '/images/seed/exhibition-5.jpg',
 DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 1),
(6, 3, '苏作匠心：江南工艺展',
 '玉雕、竹刻、缂丝与家具，呈现江南工匠的材料智慧。',
 '/images/seed/exhibition-6.jpg',
 DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 70 DAY), 1);

INSERT INTO ticket_type (id, museum_id, name, price, ticket_kind, exhibition_id, audience_type,
                         tagline, audience, description, benefits, rules, status, sort) VALUES
(1, 1, '普通预约票', 0.00, 'BASIC', NULL, 'GENERAL', '基础入馆', '所有观众',
 '免费预约故宫博物院常设展，含基础开放区域',
 '常设展与基本陈列\n基础开放区域\n免费语音导览（需自备耳机）',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 300),
(2, 1, '单展票 · 千里江山：宋代绘画特展', 40.00, 'SINGLE', 1, 'ADULT', '基础入馆 + 单展', '成人观众',
 '含基础入馆与「千里江山：宋代绘画特展」单展参观权益',
 '基础入馆\n「千里江山：宋代绘画特展」单展\n电子导览手册',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 220),
(3, 1, '单展票 · 千里江山：宋代绘画特展', 20.00, 'SINGLE', 1, 'STUDENT', '基础入馆 + 单展', '全日制本科及以下在校学生',
 '含基础入馆与「千里江山：宋代绘画特展」单展参观权益',
 '基础入馆\n「千里江山：宋代绘画特展」单展\n电子导览手册',
 '入馆须出示本人学生证\n研究生、成人教育不适用\n不与其他优惠叠加', 1, 220),
(4, 1, '单展票 · 千里江山：宋代绘画特展', 60.00, 'SINGLE', 1, 'FAMILY', '基础入馆 + 单展', '1 位成人 + 1 位 1.2–1.5 米儿童',
 '含基础入馆与「千里江山：宋代绘画特展」单展参观权益',
 '基础入馆\n「千里江山：宋代绘画特展」单展\n电子导览手册',
 '亲子票含 1 位成人 + 1 位儿童\n儿童须与成人同时入馆\n超出人数需另购票', 1, 220),
(5, 1, '单展票 · 宫城记忆：明清宫廷生活展', 40.00, 'SINGLE', 2, 'ADULT', '基础入馆 + 单展', '成人观众',
 '含基础入馆与「宫城记忆：明清宫廷生活展」单展参观权益',
 '基础入馆\n「宫城记忆：明清宫廷生活展」单展\n电子导览手册',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 210),
(6, 1, '单展票 · 宫城记忆：明清宫廷生活展', 20.00, 'SINGLE', 2, 'STUDENT', '基础入馆 + 单展', '全日制本科及以下在校学生',
 '含基础入馆与「宫城记忆：明清宫廷生活展」单展参观权益',
 '基础入馆\n「宫城记忆：明清宫廷生活展」单展\n电子导览手册',
 '入馆须出示本人学生证\n研究生、成人教育不适用\n不与其他优惠叠加', 1, 210),
(7, 1, '单展票 · 宫城记忆：明清宫廷生活展', 60.00, 'SINGLE', 2, 'FAMILY', '基础入馆 + 单展', '1 位成人 + 1 位 1.2–1.5 米儿童',
 '含基础入馆与「宫城记忆：明清宫廷生活展」单展参观权益',
 '基础入馆\n「宫城记忆：明清宫廷生活展」单展\n电子导览手册',
 '亲子票含 1 位成人 + 1 位儿童\n儿童须与成人同时入馆\n超出人数需另购票', 1, 210),
(8, 1, '全馆联票', 60.00, 'COMBO', NULL, 'ADULT', '基础入馆 + 全馆展览', '成人观众',
 '含基础入馆与故宫博物院馆内全部展览参观权益',
 '基础入馆\n馆内全部展览\n电子导览手册',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 100),
(9, 1, '全馆联票', 30.00, 'COMBO', NULL, 'STUDENT', '基础入馆 + 全馆展览', '全日制本科及以下在校学生',
 '含基础入馆与故宫博物院馆内全部展览参观权益',
 '基础入馆\n馆内全部展览\n电子导览手册',
 '入馆须出示本人学生证\n研究生、成人教育不适用\n不与其他优惠叠加', 1, 100),
(10, 1, '全馆联票', 90.00, 'COMBO', NULL, 'FAMILY', '基础入馆 + 全馆展览', '1 位成人 + 1 位 1.2–1.5 米儿童',
 '含基础入馆与故宫博物院馆内全部展览参观权益',
 '基础入馆\n馆内全部展览\n电子导览手册',
 '亲子票含 1 位成人 + 1 位儿童\n儿童须与成人同时入馆\n超出人数需另购票', 1, 100),
(11, 2, '普通预约票', 0.00, 'BASIC', NULL, 'GENERAL', '基础入馆', '所有观众',
 '免费预约上海博物馆常设展，含基础开放区域',
 '常设展与专题陈列\n基础开放区域\n免费语音导览（需自备耳机）',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 300),
(12, 2, '单展票 · 青铜之光：商周礼器特展', 50.00, 'SINGLE', 3, 'ADULT', '基础入馆 + 单展', '成人观众',
 '含基础入馆与「青铜之光：商周礼器特展」单展参观权益',
 '基础入馆\n「青铜之光：商周礼器特展」单展\n电子导览手册',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 220),
(13, 2, '单展票 · 青铜之光：商周礼器特展', 25.00, 'SINGLE', 3, 'STUDENT', '基础入馆 + 单展', '全日制本科及以下在校学生',
 '含基础入馆与「青铜之光：商周礼器特展」单展参观权益',
 '基础入馆\n「青铜之光：商周礼器特展」单展\n电子导览手册',
 '入馆须出示本人学生证\n研究生、成人教育不适用\n不与其他优惠叠加', 1, 220),
(14, 2, '单展票 · 青铜之光：商周礼器特展', 75.00, 'SINGLE', 3, 'FAMILY', '基础入馆 + 单展', '1 位成人 + 1 位 1.2–1.5 米儿童',
 '含基础入馆与「青铜之光：商周礼器特展」单展参观权益',
 '基础入馆\n「青铜之光：商周礼器特展」单展\n电子导览手册',
 '亲子票含 1 位成人 + 1 位儿童\n儿童须与成人同时入馆\n超出人数需另购票', 1, 220),
(15, 2, '单展票 · 海上风华：近代上海设计展', 50.00, 'SINGLE', 4, 'ADULT', '基础入馆 + 单展', '成人观众',
 '含基础入馆与「海上风华：近代上海设计展」单展参观权益',
 '基础入馆\n「海上风华：近代上海设计展」单展\n电子导览手册',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 210),
(16, 2, '单展票 · 海上风华：近代上海设计展', 25.00, 'SINGLE', 4, 'STUDENT', '基础入馆 + 单展', '全日制本科及以下在校学生',
 '含基础入馆与「海上风华：近代上海设计展」单展参观权益',
 '基础入馆\n「海上风华：近代上海设计展」单展\n电子导览手册',
 '入馆须出示本人学生证\n研究生、成人教育不适用\n不与其他优惠叠加', 1, 210),
(17, 2, '单展票 · 海上风华：近代上海设计展', 75.00, 'SINGLE', 4, 'FAMILY', '基础入馆 + 单展', '1 位成人 + 1 位 1.2–1.5 米儿童',
 '含基础入馆与「海上风华：近代上海设计展」单展参观权益',
 '基础入馆\n「海上风华：近代上海设计展」单展\n电子导览手册',
 '亲子票含 1 位成人 + 1 位儿童\n儿童须与成人同时入馆\n超出人数需另购票', 1, 210),
(18, 2, '全馆联票', 80.00, 'COMBO', NULL, 'ADULT', '基础入馆 + 全馆展览', '成人观众',
 '含基础入馆与上海博物馆馆内全部展览参观权益',
 '基础入馆\n馆内全部展览\n电子导览手册',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 100),
(19, 2, '全馆联票', 40.00, 'COMBO', NULL, 'STUDENT', '基础入馆 + 全馆展览', '全日制本科及以下在校学生',
 '含基础入馆与上海博物馆馆内全部展览参观权益',
 '基础入馆\n馆内全部展览\n电子导览手册',
 '入馆须出示本人学生证\n研究生、成人教育不适用\n不与其他优惠叠加', 1, 100),
(20, 2, '全馆联票', 120.00, 'COMBO', NULL, 'FAMILY', '基础入馆 + 全馆展览', '1 位成人 + 1 位 1.2–1.5 米儿童',
 '含基础入馆与上海博物馆馆内全部展览参观权益',
 '基础入馆\n馆内全部展览\n电子导览手册',
 '亲子票含 1 位成人 + 1 位儿童\n儿童须与成人同时入馆\n超出人数需另购票', 1, 100),
(21, 3, '普通预约票', 0.00, 'BASIC', NULL, 'GENERAL', '基础入馆', '所有观众',
 '免费预约苏州博物馆常设展，含基础开放区域',
 '常设展与基本陈列\n基础开放区域\n免费语音导览（需自备耳机）',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 300),
(22, 3, '单展票 · 吴门四家：文人书画特展', 45.00, 'SINGLE', 5, 'ADULT', '基础入馆 + 单展', '成人观众',
 '含基础入馆与「吴门四家：文人书画特展」单展参观权益',
 '基础入馆\n「吴门四家：文人书画特展」单展\n电子导览手册',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 220),
(23, 3, '单展票 · 吴门四家：文人书画特展', 22.00, 'SINGLE', 5, 'STUDENT', '基础入馆 + 单展', '全日制本科及以下在校学生',
 '含基础入馆与「吴门四家：文人书画特展」单展参观权益',
 '基础入馆\n「吴门四家：文人书画特展」单展\n电子导览手册',
 '入馆须出示本人学生证\n研究生、成人教育不适用\n不与其他优惠叠加', 1, 220),
(24, 3, '单展票 · 吴门四家：文人书画特展', 67.50, 'SINGLE', 5, 'FAMILY', '基础入馆 + 单展', '1 位成人 + 1 位 1.2–1.5 米儿童',
 '含基础入馆与「吴门四家：文人书画特展」单展参观权益',
 '基础入馆\n「吴门四家：文人书画特展」单展\n电子导览手册',
 '亲子票含 1 位成人 + 1 位儿童\n儿童须与成人同时入馆\n超出人数需另购票', 1, 220),
(25, 3, '单展票 · 苏作匠心：江南工艺展', 45.00, 'SINGLE', 6, 'ADULT', '基础入馆 + 单展', '成人观众',
 '含基础入馆与「苏作匠心：江南工艺展」单展参观权益',
 '基础入馆\n「苏作匠心：江南工艺展」单展\n电子导览手册',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 210),
(26, 3, '单展票 · 苏作匠心：江南工艺展', 22.00, 'SINGLE', 6, 'STUDENT', '基础入馆 + 单展', '全日制本科及以下在校学生',
 '含基础入馆与「苏作匠心：江南工艺展」单展参观权益',
 '基础入馆\n「苏作匠心：江南工艺展」单展\n电子导览手册',
 '入馆须出示本人学生证\n研究生、成人教育不适用\n不与其他优惠叠加', 1, 210),
(27, 3, '单展票 · 苏作匠心：江南工艺展', 67.50, 'SINGLE', 6, 'FAMILY', '基础入馆 + 单展', '1 位成人 + 1 位 1.2–1.5 米儿童',
 '含基础入馆与「苏作匠心：江南工艺展」单展参观权益',
 '基础入馆\n「苏作匠心：江南工艺展」单展\n电子导览手册',
 '亲子票含 1 位成人 + 1 位儿童\n儿童须与成人同时入馆\n超出人数需另购票', 1, 210),
(28, 3, '全馆联票', 70.00, 'COMBO', NULL, 'ADULT', '基础入馆 + 全馆展览', '成人观众',
 '含基础入馆与苏州博物馆馆内全部展览参观权益',
 '基础入馆\n馆内全部展览\n电子导览手册',
 '每单最多 6 人\n入馆须携带本人有效身份证件\n参观时段开始前可取消', 1, 100),
(29, 3, '全馆联票', 35.00, 'COMBO', NULL, 'STUDENT', '基础入馆 + 全馆展览', '全日制本科及以下在校学生',
 '含基础入馆与苏州博物馆馆内全部展览参观权益',
 '基础入馆\n馆内全部展览\n电子导览手册',
 '入馆须出示本人学生证\n研究生、成人教育不适用\n不与其他优惠叠加', 1, 100),
(30, 3, '全馆联票', 105.00, 'COMBO', NULL, 'FAMILY', '基础入馆 + 全馆展览', '1 位成人 + 1 位 1.2–1.5 米儿童',
 '含基础入馆与苏州博物馆馆内全部展览参观权益',
 '基础入馆\n馆内全部展览\n电子导览手册',
 '亲子票含 1 位成人 + 1 位儿童\n儿童须与成人同时入馆\n超出人数需另购票', 1, 100);

-- 未来 14 天、每个博物馆每天 3 个时段
INSERT INTO visit_slot (museum_id, exhibition_id, visit_date, start_time, end_time, total_capacity, booked_count, status)
SELECT m.id, NULL, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 7 DAY), INTERVAL d.n DAY),
       t.start_time, t.end_time, t.capacity, 0,
       CASE WHEN DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 7 DAY), INTERVAL d.n DAY) < CURDATE()
                 OR (DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 7 DAY), INTERVAL d.n DAY) = CURDATE() AND t.start_time <= CURTIME())
            THEN 0 ELSE 1 END
FROM museum m
JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
      UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
      UNION ALL SELECT 13) d
JOIN (SELECT CAST('09:00:00' AS TIME) AS start_time, CAST('11:00:00' AS TIME) AS end_time, 300 AS capacity
      UNION ALL
      SELECT CAST('13:00:00' AS TIME), CAST('15:00:00' AS TIME), 200
      UNION ALL
      SELECT CAST('18:00:00' AS TIME), CAST('20:00:00' AS TIME), 150) t;

-- ============================================================
-- 公告、参数
-- ============================================================
INSERT INTO notice (title, summary, content, cover_url, status, publish_time) VALUES
('预约须知：实名、分时、限量',
 '请提前完成实名信息填写，并按预约时段到馆。',
 '为保障参观体验，本系统实行实名制、分时段预约。请如实填写参观者姓名与身份证号，入馆时出示电子票二维码并配合核验。每个订单最多可预约 6 人。如行程变化，请在参观时段开始前取消预约，释放票额给其他观众。',
 '/images/seed/notice-1.jpg', 1, NOW()),
('秋季特展集中开放',
 '三家博物馆同期推出重量级特展，建议错峰预约。',
 '本周起，故宫博物院“千里江山”、上海博物馆“青铜之光”、苏州博物馆“吴门四家”三大特展同步开放。特展票数量有限，请提前选择时段预约。夜场活动以各馆公告为准。',
 '/images/seed/notice-2.jpg', 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('文明参观提示',
 '请勿触摸展品，展厅内请保持安静。',
 '进入展厅前请将饮品存入寄存处；展厅内请勿使用闪光灯、自拍杆与三脚架；请与展柜保持适当距离。携带儿童参观的观众请照看好孩子，共同维护良好的参观环境。',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 3 DAY));

INSERT INTO system_config (config_key, config_value, description) VALUES
('site_name', '云观博物馆预约平台', '站点名称'),
('visit_notice', '请提前 10 分钟到馆，凭电子票和有效身份证件入场。', '预约成功页提示'),
('max_visitors_per_order', '6', '单笔订单最多参观人数'),
('advance_booking_days', '14', '可提前预约天数'),
('payment_timeout_minutes', '15', '模拟支付超时时间（分钟）'),
('maintenance_mode', 'false', '维护模式开关');

-- ============================================================
-- 演示预约数据（便于统计报表与核销记录展示）
-- ============================================================
SET @slot_m1_today = (SELECT id FROM visit_slot WHERE museum_id = 1 AND visit_date = CURDATE() AND start_time = '09:00:00' LIMIT 1);
SET @slot_m2_today = (SELECT id FROM visit_slot WHERE museum_id = 2 AND visit_date = CURDATE() AND start_time = '13:00:00' LIMIT 1);
SET @slot_m1_tomorrow = (SELECT id FROM visit_slot WHERE museum_id = 1 AND visit_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) AND start_time = '09:00:00' LIMIT 1);

UPDATE visit_slot SET booked_count = 2 WHERE id = @slot_m1_today;
UPDATE visit_slot SET booked_count = 1 WHERE id = @slot_m2_today;

INSERT INTO reservation (id, order_no, user_id, museum_id, exhibition_id, slot_id, ticket_type_id,
                         visitor_count, unit_price, total_amount, contact_name, contact_phone,
                         status, qr_expire_at, checkin_time, created_at)
VALUES
(1, 'MRDEMO20260001', 3, 1, NULL, @slot_m1_today, 1, 2, 0.00, 0.00, '林小满', '13900000003',
 'CHECKED_IN', TIMESTAMP(CURDATE(), '11:30:00'), TIMESTAMP(CURDATE(), '09:18:00'), TIMESTAMP(CURDATE(), '08:20:00')),
(2, 'MRDEMO20260002', 3, 2, NULL, @slot_m2_today, 18, 1, 80.00, 80.00, '林小满', '13900000003',
 'CHECKED_IN', TIMESTAMP(CURDATE(), '15:30:00'), TIMESTAMP(CURDATE(), '13:06:00'), TIMESTAMP(CURDATE(), '12:10:00')),
(3, 'MRDEMO20260003', 3, 1, NULL, @slot_m1_tomorrow, 8, 1, 60.00, 60.00, '林小满', '13900000003',
 'CANCELED', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:30:00'), NULL,
 TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '16:30:00'));

UPDATE reservation SET cancel_time = TIMESTAMP(CURDATE(), '08:05:00') WHERE id = 3;

INSERT INTO visitor (reservation_id, slot_id, name, id_card, phone) VALUES
(1, @slot_m1_today, '林小满', '110101199505050033', '13900000003'),
(1, @slot_m1_today, '周予安', '110101199806060044', '13900000004'),
(2, @slot_m2_today, '林小满', '110101199505050033', '13900000003'),
(3, @slot_m1_tomorrow, '林小满', '110101199505050033', '13900000003');

INSERT INTO slot_guest_lock (slot_id, id_card, reservation_id) VALUES
(@slot_m1_today, '110101199505050033', 1),
(@slot_m1_today, '110101199806060044', 1),
(@slot_m2_today, '110101199505050033', 2);

INSERT INTO payment_order (payment_no, order_no, reservation_id, user_id, amount, pay_type, status, paid_time) VALUES
('PAYDEMO20260001', 'MRDEMO20260001', 1, 3, 0.00, 'FREE', 'PAID', TIMESTAMP(CURDATE(), '08:20:30')),
('PAYDEMO20260002', 'MRDEMO20260002', 2, 3, 80.00, 'MOCK', 'PAID', TIMESTAMP(CURDATE(), '12:11:00')),
('PAYDEMO20260003', 'MRDEMO20260003', 3, 3, 60.00, 'MOCK', 'REFUNDED', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '16:31:00'));

UPDATE payment_order SET refund_time = TIMESTAMP(CURDATE(), '08:05:00') WHERE reservation_id = 3;

INSERT INTO checkin_record (reservation_id, order_no, checkin_time, gate, operator_id, operator_name, status) VALUES
(1, 'MRDEMO20260001', TIMESTAMP(CURDATE(), '09:18:00'), '午门东闸机', 2, '预约管理员', 'SUCCESS'),
(2, 'MRDEMO20260002', TIMESTAMP(CURDATE(), '13:06:00'), '南门闸机', 2, '预约管理员', 'SUCCESS');

INSERT INTO operation_log (user_id, username, module, operation, method, params, ip, duration_ms, success, create_time) VALUES
(2, 'manager', '核销', '票码核销', 'CheckinController.verify', '[{"ticketCode":"***"}]', '127.0.0.1', 86, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:18:00')),
(3, 'visitor', '预约', '提交预约', 'ReservationController.create', '[{"slotId":1,"ticketTypeId":1}]', '127.0.0.1', 132, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '08:20:00')),
(1, 'admin', '系统管理', '修改参数配置', 'ConfigAdminController.update', '[{"configValue":"6"}]', '127.0.0.1', 41, 1, DATE_SUB(NOW(), INTERVAL 6 DAY) - INTERVAL 2 HOUR),
(1, 'admin', '用户管理', '新增用户', 'UserAdminController.create', '[{"username":"visitor"}]', '127.0.0.1', 58, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '10:12:00')),
(2, 'manager', '展览管理', '新增展览', 'ExhibitionAdminController.create', '[{"name":"千里江山：宋代绘画特展"}]', '127.0.0.1', 73, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '14:35:00')),
(1, 'admin', '系统管理', '新增博物馆', 'MuseumAdminController.create', '[{"name":"故宫博物院"}]', '127.0.0.1', 64, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:08:00')),
(2, 'manager', '时段管理', '新增参观时段', 'SlotAdminController.create', '[{"museumId":1}]', '127.0.0.1', 91, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '11:20:00')),
(3, 'visitor', '预约', '提交预约', 'ReservationController.create', '[{"slotId":2,"ticketTypeId":18}]', '127.0.0.1', 118, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '13:45:00')),
(2, 'manager', '核销', '票码核销', 'CheckinController.verify', '[{"ticketCode":"***"}]', '127.0.0.1', 79, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:30:00')),
(1, 'admin', '系统管理', '修改参数配置', 'ConfigAdminController.update', '[{"configValue":"14"}]', '127.0.0.1', 36, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '16:05:00')),
(3, 'visitor', '预约', '取消预约', 'ReservationController.cancel', '[{"orderNo":"MRDEMO20260003"}]', '127.0.0.1', 95, 1, TIMESTAMP(CURDATE(), '15:40:00')),
(1, 'admin', '日志管理', '查询操作日志', 'LogAdminController.page', '[{"page":1}]', '127.0.0.1', 27, 1, TIMESTAMP(CURDATE(), '20:10:00')),
(1, 'admin', '统计', '查看统计报表', 'StatisticsController.overview', '[{}]', '127.0.0.1', 45, 1, TIMESTAMP(CURDATE(), '09:00:00'));

-- ============================================================
-- 补充 10/20 ~ 10/26 预约记录（游客 + 管理员）
-- ============================================================
-- ============================================================
-- 补充 10/20 ~ 10/26 的预约记录（游客 + 管理员），并补充操作日志
-- ============================================================
USE museum_reservation;

-- 预约 4：10/20 故宫 09:00，游客，已核销
INSERT INTO reservation (id, order_no, user_id, museum_id, exhibition_id, slot_id, ticket_type_id,
                         visitor_count, unit_price, total_amount, contact_name, contact_phone,
                         status, qr_expire_at, checkin_time, created_at)
SELECT 13, 'MRDEMO20260004', 3, 1, NULL,
       (SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND start_time='09:00:00' LIMIT 1),
       1, 2, 0.00, 0.00, '林小满', '13900000003',
       'CHECKED_IN', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),'11:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),'09:12:00'),
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),'08:15:00');
UPDATE visit_slot SET booked_count = booked_count + 2
  WHERE id = (SELECT id FROM (SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND start_time='09:00:00' LIMIT 1) t);
INSERT INTO visitor (reservation_id, slot_id, name, id_card, phone) VALUES
(13, (SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND start_time='09:00:00' LIMIT 1), '林小满', '110101199505050033', '13900000003'),
(13, (SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND start_time='09:00:00' LIMIT 1), '周予安', '110101199806060044', '13900000004');
INSERT INTO slot_guest_lock (slot_id, id_card, reservation_id) VALUES
((SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND start_time='09:00:00' LIMIT 1), '110101199505050033', 13),
((SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND start_time='09:00:00' LIMIT 1), '110101199806060044', 13);
INSERT INTO payment_order (payment_no, order_no, reservation_id, user_id, amount, pay_type, status, paid_time) VALUES
('PAYDEMO20260004', 'MRDEMO20260004', 13, 3, 0.00, 'FREE', 'PAID', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),'08:15:30'));
INSERT INTO checkin_record (reservation_id, order_no, checkin_time, gate, operator_id, operator_name, status) VALUES
(13, 'MRDEMO20260004', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),'09:12:00'), '午门东闸机', 2, '预约管理员', 'SUCCESS');

-- 预约 5：10/22 上海 09:00，管理员，已核销
INSERT INTO reservation (id, order_no, user_id, museum_id, exhibition_id, slot_id, ticket_type_id,
                         visitor_count, unit_price, total_amount, contact_name, contact_phone,
                         status, qr_expire_at, checkin_time, created_at)
SELECT 14, 'MRDEMO20260005', 2, 2, NULL,
       (SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 5 DAY) AND start_time='09:00:00' LIMIT 1),
       18, 1, 80.00, 80.00, '预约管理员', '13800000002',
       'CHECKED_IN', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'11:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'09:05:00'),
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'08:40:00');
UPDATE visit_slot SET booked_count = booked_count + 1
  WHERE id = (SELECT id FROM (SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 5 DAY) AND start_time='09:00:00' LIMIT 1) t);
INSERT INTO visitor (reservation_id, slot_id, name, id_card, phone) VALUES
(14, (SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 5 DAY) AND start_time='09:00:00' LIMIT 1), '预约管理员', '110101199202020022', '13800000002');
INSERT INTO slot_guest_lock (slot_id, id_card, reservation_id) VALUES
((SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 5 DAY) AND start_time='09:00:00' LIMIT 1), '110101199202020022', 14);
INSERT INTO payment_order (payment_no, order_no, reservation_id, user_id, amount, pay_type, status, paid_time) VALUES
('PAYDEMO20260005', 'MRDEMO20260005', 14, 2, 80.00, 'MOCK', 'PAID', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'08:41:00'));
INSERT INTO checkin_record (reservation_id, order_no, checkin_time, gate, operator_id, operator_name, status) VALUES
(14, 'MRDEMO20260005', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'09:05:00'), '南门闸机', 2, '预约管理员', 'SUCCESS');

-- 预约 6：10/24 苏州 13:00，游客，已预约（未核销）
INSERT INTO reservation (id, order_no, user_id, museum_id, exhibition_id, slot_id, ticket_type_id,
                         visitor_count, unit_price, total_amount, contact_name, contact_phone,
                         status, qr_expire_at, checkin_time, created_at)
SELECT 15, 'MRDEMO20260006', 3, 3, NULL,
       (SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 3 DAY) AND start_time='13:00:00' LIMIT 1),
       22, 1, 45.00, 45.00, '林小满', '13900000003',
       'RESERVED', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY),'15:30:00'), NULL,
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY),'10:20:00');
UPDATE visit_slot SET booked_count = booked_count + 1
  WHERE id = (SELECT id FROM (SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 3 DAY) AND start_time='13:00:00' LIMIT 1) t);
INSERT INTO visitor (reservation_id, slot_id, name, id_card, phone) VALUES
(15, (SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 3 DAY) AND start_time='13:00:00' LIMIT 1), '林小满', '110101199505050033', '13900000003');
INSERT INTO slot_guest_lock (slot_id, id_card, reservation_id) VALUES
((SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 3 DAY) AND start_time='13:00:00' LIMIT 1), '110101199505050033', 15);
INSERT INTO payment_order (payment_no, order_no, reservation_id, user_id, amount, pay_type, status, paid_time) VALUES
('PAYDEMO20260006', 'MRDEMO20260006', 15, 3, 45.00, 'MOCK', 'PAID', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY),'10:21:00'));

-- 预约 7：10/26 故宫 18:00，管理员，已取消
INSERT INTO reservation (id, order_no, user_id, museum_id, exhibition_id, slot_id, ticket_type_id,
                         visitor_count, unit_price, total_amount, contact_name, contact_phone,
                         status, qr_expire_at, checkin_time, created_at)
SELECT 16, 'MRDEMO20260007', 2, 1, NULL,
       (SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND start_time='18:00:00' LIMIT 1),
       8, 1, 60.00, 60.00, '预约管理员', '13800000002',
       'CANCELED', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY),'20:30:00'), NULL,
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY),'15:00:00');
INSERT INTO visitor (reservation_id, slot_id, name, id_card, phone) VALUES
(16, (SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND start_time='18:00:00' LIMIT 1), '预约管理员', '110101199202020022', '13800000002');
INSERT INTO payment_order (payment_no, order_no, reservation_id, user_id, amount, pay_type, status, paid_time) VALUES
('PAYDEMO20260007', 'MRDEMO20260007', 16, 2, 60.00, 'MOCK', 'REFUNDED', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY),'15:01:00'));
UPDATE payment_order SET refund_time = TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY),'15:30:00') WHERE reservation_id = 16;
UPDATE reservation SET cancel_time = TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY),'15:30:00') WHERE id = 16;

-- 补充操作日志（分散在 10/20 ~ 10/27）
INSERT INTO operation_log (user_id, username, module, operation, method, params, ip, duration_ms, success, create_time) VALUES
(1, 'admin', '用户管理', '新增用户', 'UserAdminController.create', '[{"username":"visitor"}]', '127.0.0.1', 58, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),'10:12:00')),
(2, 'manager', '展览管理', '新增展览', 'ExhibitionAdminController.create', '[{"name":"千里江山：宋代绘画特展"}]', '127.0.0.1', 73, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),'14:35:00')),
(1, 'admin', '系统管理', '新增博物馆', 'MuseumAdminController.create', '[{"name":"故宫博物院"}]', '127.0.0.1', 64, 1, TIMESTAMP('2026-10-21','09:08:00')),
(2, 'manager', '时段管理', '新增参观时段', 'SlotAdminController.create', '[{"museumId":1}]', '127.0.0.1', 91, 1, TIMESTAMP('2026-10-21','11:20:00')),
(3, 'visitor', '预约', '提交预约', 'ReservationController.create', '[{"slotId":2,"ticketTypeId":18}]', '127.0.0.1', 118, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'13:45:00')),
(2, 'manager', '核销', '票码核销', 'CheckinController.verify', '[{"ticketCode":"***"}]', '127.0.0.1', 79, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'10:30:00')),
(1, 'admin', '系统管理', '修改参数配置', 'ConfigAdminController.update', '[{"configValue":"14"}]', '127.0.0.1', 36, 1, TIMESTAMP('2026-10-23','16:05:00')),
(3, 'visitor', '预约', '取消预约', 'ReservationController.cancel', '[{"orderNo":"MRDEMO20260003"}]', '127.0.0.1', 95, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY),'15:40:00')),
(1, 'admin', '日志管理', '查询操作日志', 'LogAdminController.page', '[{"page":1}]', '127.0.0.1', 27, 1, TIMESTAMP('2026-10-25','20:10:00')),
(2, 'manager', '预约', '提交预约', 'ReservationController.create', '[{"slotId":14,"ticketTypeId":18}]', '127.0.0.1', 121, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'08:40:00')),
(3, 'visitor', '预约', '提交预约', 'ReservationController.create', '[{"slotId":15,"ticketTypeId":22}]', '127.0.0.1', 109, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY),'10:20:00')),
(2, 'manager', '预约', '取消预约', 'ReservationController.cancel', '[{"orderNo":"MRDEMO20260007"}]', '127.0.0.1', 88, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY),'15:30:00')),
(1, 'admin', '系统管理', '修改参数配置', 'ConfigAdminController.update', '[{"configValue":"6"}]', '127.0.0.1', 41, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY),'18:20:00')),
(3, 'visitor', '预约', '提交预约', 'ReservationController.create', '[{"slotId":16,"ticketTypeId":1}]', '127.0.0.1', 132, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),'08:15:00')),
(2, 'manager', '核销', '票码核销', 'CheckinController.verify', '[{"ticketCode":"***"}]', '127.0.0.1', 83, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),'09:12:00')),
(1, 'admin', '统计', '查看统计报表', 'StatisticsController.overview', '[{}]', '127.0.0.1', 45, 1, TIMESTAMP('2026-10-27','09:00:00'));

-- ============================================================
-- 补充游客在其他日期的预约记录 + 操作日志
-- ============================================================
-- ============================================================
-- 补充游客在其他日期的预约记录（wsm/visitor）+ 操作日志
-- ============================================================
USE museum_reservation;

-- 预约 8：10/21 上海 13:00，游客，已预约（过期）
INSERT INTO reservation (id, order_no, user_id, museum_id, exhibition_id, slot_id, ticket_type_id,
                         visitor_count, unit_price, total_amount, contact_name, contact_phone,
                         status, qr_expire_at, checkin_time, created_at)
SELECT 17, 'MRDEMO20260008', 3, 2, NULL,
       (SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND start_time='13:00:00' LIMIT 1),
       12, 1, 50.00, 50.00, '林小满', '13900000003',
       'RESERVED', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY),'15:30:00'), NULL,
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY),'10:05:00');
UPDATE visit_slot SET booked_count = booked_count + 1
  WHERE id = (SELECT id FROM (SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND start_time='13:00:00' LIMIT 1) t);
INSERT INTO visitor (reservation_id, slot_id, name, id_card, phone) VALUES
(17, (SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND start_time='13:00:00' LIMIT 1), '林小满', '110101199505050033', '13900000003');
INSERT INTO slot_guest_lock (slot_id, id_card, reservation_id) VALUES
((SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND start_time='13:00:00' LIMIT 1), '110101199505050033', 17);
INSERT INTO payment_order (payment_no, order_no, reservation_id, user_id, amount, pay_type, status, paid_time) VALUES
('PAYDEMO20260008', 'MRDEMO20260008', 17, 3, 50.00, 'MOCK', 'PAID', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY),'10:06:00'));

-- 预约 9：10/23 苏州 18:00，游客，已核销（2人）
INSERT INTO reservation (id, order_no, user_id, museum_id, exhibition_id, slot_id, ticket_type_id,
                         visitor_count, unit_price, total_amount, contact_name, contact_phone,
                         status, qr_expire_at, checkin_time, created_at)
SELECT 18, 'MRDEMO20260009', 3, 3, NULL,
       (SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 4 DAY) AND start_time='18:00:00' LIMIT 1),
       25, 2, 45.00, 90.00, '林小满', '13900000003',
       'CHECKED_IN', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY),'20:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY),'18:08:00'),
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY),'16:20:00');
UPDATE visit_slot SET booked_count = booked_count + 2
  WHERE id = (SELECT id FROM (SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 4 DAY) AND start_time='18:00:00' LIMIT 1) t);
INSERT INTO visitor (reservation_id, slot_id, name, id_card, phone) VALUES
(18, (SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 4 DAY) AND start_time='18:00:00' LIMIT 1), '林小满', '110101199505050033', '13900000003'),
(18, (SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 4 DAY) AND start_time='18:00:00' LIMIT 1), '周予安', '110101199806060044', '13900000004');
INSERT INTO slot_guest_lock (slot_id, id_card, reservation_id) VALUES
((SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 4 DAY) AND start_time='18:00:00' LIMIT 1), '110101199505050033', 18),
((SELECT id FROM visit_slot WHERE museum_id=3 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 4 DAY) AND start_time='18:00:00' LIMIT 1), '110101199806060044', 18);
INSERT INTO payment_order (payment_no, order_no, reservation_id, user_id, amount, pay_type, status, paid_time) VALUES
('PAYDEMO20260009', 'MRDEMO20260009', 18, 3, 90.00, 'MOCK', 'PAID', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY),'16:21:00'));
INSERT INTO checkin_record (reservation_id, order_no, checkin_time, gate, operator_id, operator_name, status) VALUES
(18, 'MRDEMO20260009', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY),'18:08:00'), '东北街闸机', 2, '预约管理员', 'SUCCESS');

-- 预约 10：10/25 故宫 09:00，游客，已取消
INSERT INTO reservation (id, order_no, user_id, museum_id, exhibition_id, slot_id, ticket_type_id,
                         visitor_count, unit_price, total_amount, contact_name, contact_phone,
                         status, qr_expire_at, checkin_time, created_at)
SELECT 19, 'MRDEMO20260010', 3, 1, NULL,
       (SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 2 DAY) AND start_time='09:00:00' LIMIT 1),
       2, 1, 40.00, 40.00, '林小满', '13900000003',
       'CANCELED', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY),'11:30:00'), NULL,
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY),'08:50:00');
INSERT INTO visitor (reservation_id, slot_id, name, id_card, phone) VALUES
(19, (SELECT id FROM visit_slot WHERE museum_id=1 AND visit_date=DATE_SUB(CURDATE(), INTERVAL 2 DAY) AND start_time='09:00:00' LIMIT 1), '林小满', '110101199505050033', '13900000003');
INSERT INTO payment_order (payment_no, order_no, reservation_id, user_id, amount, pay_type, status, paid_time) VALUES
('PAYDEMO20260010', 'MRDEMO20260010', 19, 3, 40.00, 'MOCK', 'REFUNDED', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY),'08:51:00'));
UPDATE payment_order SET refund_time = TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY),'09:00:00') WHERE reservation_id = 19;
UPDATE reservation SET cancel_time = TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY),'09:00:00') WHERE id = 19;

-- 预约 11：10/27 上海 18:00，游客，已预约
INSERT INTO reservation (id, order_no, user_id, museum_id, exhibition_id, slot_id, ticket_type_id,
                         visitor_count, unit_price, total_amount, contact_name, contact_phone,
                         status, qr_expire_at, checkin_time, created_at)
SELECT 20, 'MRDEMO20260011', 3, 2, NULL,
       (SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=CURDATE() AND start_time='18:00:00' LIMIT 1),
       18, 1, 80.00, 80.00, '林小满', '13900000003',
       'RESERVED', TIMESTAMP(CURDATE(),'20:30:00'), NULL,
       TIMESTAMP(CURDATE(),'17:10:00');
UPDATE visit_slot SET booked_count = booked_count + 1
  WHERE id = (SELECT id FROM (SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=CURDATE() AND start_time='18:00:00' LIMIT 1) t);
INSERT INTO visitor (reservation_id, slot_id, name, id_card, phone) VALUES
(20, (SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=CURDATE() AND start_time='18:00:00' LIMIT 1), '林小满', '110101199505050033', '13900000003');
INSERT INTO slot_guest_lock (slot_id, id_card, reservation_id) VALUES
((SELECT id FROM visit_slot WHERE museum_id=2 AND visit_date=CURDATE() AND start_time='18:00:00' LIMIT 1), '110101199505050033', 20);
INSERT INTO payment_order (payment_no, order_no, reservation_id, user_id, amount, pay_type, status, paid_time) VALUES
('PAYDEMO20260011', 'MRDEMO20260011', 20, 3, 80.00, 'MOCK', 'PAID', TIMESTAMP(CURDATE(),'17:11:00'));

-- 对应操作日志
INSERT INTO operation_log (user_id, username, module, operation, method, params, ip, duration_ms, success, create_time) VALUES
(3, 'visitor', '预约', '提交预约', 'ReservationController.create', '[{"slotId":17,"ticketTypeId":12}]', '127.0.0.1', 115, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY),'10:05:00')),
(3, 'visitor', '预约', '提交预约', 'ReservationController.create', '[{"slotId":18,"ticketTypeId":25}]', '127.0.0.1', 128, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY),'16:20:00')),
(2, 'manager', '核销', '票码核销', 'CheckinController.verify', '[{"ticketCode":"***"}]', '127.0.0.1', 81, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY),'18:08:00')),
(3, 'visitor', '预约', '取消预约', 'ReservationController.cancel', '[{"orderNo":"MRDEMO20260010"}]', '127.0.0.1', 92, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY),'09:00:00')),
(3, 'visitor', '预约', '提交预约', 'ReservationController.create', '[{"slotId":20,"ticketTypeId":18}]', '127.0.0.1', 124, 1, TIMESTAMP(CURDATE(),'17:10:00'));
