CREATE TABLE users (
    id        UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(500) NOT NULL,
    email     VARCHAR(100) UNIQUE,
    full_name VARCHAR(200),
    enabled   BOOLEAN      NOT NULL DEFAULT true
);

CREATE TABLE authorities (
    username  VARCHAR(50) NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT pk_authorities PRIMARY KEY (username, authority)
);
