CREATE TABLE users
(
    email          text NOT NULL,
    hashedPassword text NOT NULL,
    firstName      text,
    lastName       text,
    company        text,
    role           text NOT NULL
);

ALTER TABLE users
    ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users(email,
                  hashedPassword,
                  firstName,
                  lastName,
                  company,
                  role)
VALUES ('daniel@rockthejvm.com', -- email
        'rockthejvm', -- hashedPassword
        'Daniel', -- firstName
        'Ciocirlan', -- lastName
        'Rock The JVM', -- company
        'ADMIN' -- role
       );

INSERT INTO users(email,
                  hashedPassword,
                  firstName,
                  lastName,
                  company,
                  role)
VALUES ('riccardo@rockthejvm.com', -- email
        'riccardorulez', -- hashedPassword
        'Riccardo', -- firstName
        'Cardin', -- lastName
        'Rock The JVM', -- company
        'RECRUITER' -- role
       );
