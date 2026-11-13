CREATE DATABASE IF NOT EXISTS online_voting;
USE online_voting;

CREATE TABLE IF NOT EXISTS voters (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    voter_id VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS candidates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    party VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS votes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    voter_id INT NOT NULL UNIQUE,
    candidate_id INT NOT NULL,
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vote_voter FOREIGN KEY (voter_id) REFERENCES voters(id),
    CONSTRAINT fk_vote_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id)
);

-- Demo candidates (run this INSERT only once)
INSERT INTO candidates (name, party) VALUES
('Aarav Sharma', 'Unity Party'),
('Meera Singh', 'Progress Party'),
('Kabir Verma', 'People First');
