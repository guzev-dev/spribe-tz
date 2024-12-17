--changeset guzev.dev@gmail.com:1
CREATE TABLE CURRENCY (
    CURRENCY_CODE                      VARCHAR(3) PRIMARY KEY
);
--rollback DROP TABLE CURRENCY;