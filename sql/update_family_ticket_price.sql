-- 亲子票价格调整为对应成人票的 1.5 倍
UPDATE ticket_type AS family
    INNER JOIN ticket_type AS adult
        ON adult.museum_id = family.museum_id
       AND adult.ticket_kind = family.ticket_kind
       AND IFNULL(adult.exhibition_id, -1) = IFNULL(family.exhibition_id, -1)
       AND adult.audience_type = 'ADULT'
       AND adult.status = 1
SET family.price = ROUND(adult.price * 1.5, 2)
WHERE family.audience_type = 'FAMILY'
  AND family.status = 1;
