CREATE TABLE allowed_email (
           id BIGINT AUTO_INCREMENT PRIMARY KEY,
           domain VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO allowed_email (domain) VALUES
                                              ('naver.com'),
                                              ('gmail.com'),
                                              ('kakao.com'),
                                              ('icloud.com'),
                                              ('hanmail.com'),
                                              ('daum.net');
