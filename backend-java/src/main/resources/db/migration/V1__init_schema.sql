-- MDB - Math Data Base - schema inicial

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE areas (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tags (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE problems (
    id                 BIGSERIAL PRIMARY KEY,
    title              VARCHAR(200) NOT NULL,
    statement          TEXT         NOT NULL,
    hint               TEXT         NOT NULL,
    solution           TEXT         NOT NULL,
    source             VARCHAR(255),
    initial_difficulty INTEGER      NOT NULL,
    area_id            BIGINT       NOT NULL REFERENCES areas(id),
    created_by         BIGINT       NOT NULL REFERENCES users(id),
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_problem_initial_difficulty CHECK (initial_difficulty BETWEEN 1 AND 5)
);

CREATE TABLE problem_tags (
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag_id     BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (problem_id, tag_id)
);

CREATE TABLE difficulty_ratings (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id  BIGINT    NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    difficulty  INTEGER   NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_difficulty_rating_user_problem UNIQUE (user_id, problem_id),
    CONSTRAINT chk_difficulty_rating_value CHECK (difficulty BETWEEN 1 AND 5)
);

CREATE TABLE user_problem_status (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id BIGINT    NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_problem_status UNIQUE (user_id, problem_id, status),
    CONSTRAINT chk_user_problem_status_value CHECK (status IN ('WANT_TO_DO', 'DONE', 'FAVORITE'))
);

CREATE INDEX idx_problems_area ON problems(area_id);
CREATE INDEX idx_problems_created_by ON problems(created_by);
CREATE INDEX idx_difficulty_ratings_problem ON difficulty_ratings(problem_id);
CREATE INDEX idx_user_problem_status_user ON user_problem_status(user_id);
CREATE INDEX idx_user_problem_status_problem ON user_problem_status(problem_id);
