CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) NOT NULL REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Appointments (
    Id int IDENTITY,
    CaregiverName varchar(255) NOT NULL REFERENCES Caregivers,
    PatientName varchar(255) NOT NULL REFERENCES Patients,
    VaccineName varchar(255) NOT NULL REFERENCES Vaccines,
    Time date,
    PRIMARY KEY (Id)
);