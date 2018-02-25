-- Find all papers
SELECT * FROM fyp.paper;
-- Find all key phrases
SELECT * FROM fyp.key_phrase;
SELECT * FROM fyp.hyponym;
SELECT * FROM fyp.syn_link;
SELECT * FROM fyp.synonym;

-- Find overlapping key phrases
SELECT kp1.* FROM key_phrase kp1, key_phrase kp2 WHERE kp1.paper = kp2.paper AND kp1.end > kp2.start AND kp1.end < kp2.end;

-- Find a certain type of key phrases
SELECT * FROM fyp.key_phrase WHERE classification = "Task";

-- Papers not finished processing
SELECT * FROM fyp.paper WHERE status != 4;
-- Finish them
UPDATE fyp.paper SET status = -1 WHERE status != 4;

SELECT * FROM paper WHERE text LIKE '%xylanases%'; # Proves default case insentivity
SELECT * FROM paper WHERE text REGEXP 'xylanases'; # Proves regex default case insentivity
