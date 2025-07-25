CREATE TABLE allowed_email_domain (
           id BIGINT AUTO_INCREMENT PRIMARY KEY,
           domain VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO allowed_email_domain (domain) VALUES
                                              ('naver.com'),
                                              ('gmail.com'),
                                              ('kakao.com'),
                                              ('icloud.com'),
                                              ('hanmail.com'),
                                              ('daum.net');
