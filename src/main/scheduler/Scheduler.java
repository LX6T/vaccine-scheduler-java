package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");
        System.out.println("> reserve <date> <vaccine>");
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");
        System.out.println("> logout");
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Create patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        String pCheckMessage = Util.passwordCheck(password);
        if (!pCheckMessage.equals("STRONG")) {
            System.out.println(pCheckMessage + ", try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        String pCheckMessage = Util.passwordCheck(password);
        if (!pCheckMessage.equals("STRONG")) {
            System.out.println(pCheckMessage + ", try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in, try again");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            System.out.println("Logged in as " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // searchCaregiverSchedule <date>
        // check 1: check if there is a user currently logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }
        String date = tokens[1];

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsernames = "SELECT A.Username FROM Availabilities AS A WHERE A.Time = ? ORDER BY A.Username";
        String selectVaccines = "SELECT * FROM Vaccines";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsernames);
            Date d = Date.valueOf(date);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                System.out.println(resultSet.getString(1));
            }

            statement = con.prepareStatement(selectVaccines);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                System.out.println(resultSet.getString(1) + " " + resultSet.getString(2));
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // searchCaregiverSchedule <date>
        // check 1: check if there is a user currently logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        } else if (currentCaregiver != null) {
            System.out.println("Please login as a patient");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }
        String date = tokens[1];
        String vaccineName = tokens[2];

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsernames = "SELECT A.Username FROM Availabilities AS A WHERE A.Time = ? ORDER BY A.Username";
        String selectVaccines = "SELECT V.Doses FROM Vaccines AS V WHERE V.Name = ? AND V.Doses > 0";
        String insertAppointment = "INSERT INTO Appointments (CaregiverName, PatientName, VaccineName, Time) VALUES (?, ?, ?, ?)";
        String selectAppointment = "SELECT A.Id FROM Appointments AS A ORDER BY A.Id DESC";
        String deleteAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsernames);
            Date d = Date.valueOf(date);
            statement.setDate(1, d);
            ResultSet caregiverSet = statement.executeQuery();
            if (!caregiverSet.next()) {
                System.out.println("No caregiver is available");
                return;
            }
            String caregiverName = caregiverSet.getString(1);

            statement = con.prepareStatement(selectVaccines);
            statement.setString(1, vaccineName);
            ResultSet vaccineSet = statement.executeQuery();
            if (!vaccineSet.next()) {
                System.out.println("Not enough available doses");
                return;
            }

            statement = con.prepareStatement(insertAppointment);
            statement.setString(1, caregiverName);
            statement.setString(2, currentPatient.getUsername());
            statement.setString(3, vaccineName);
            statement.setDate(4, d);
            statement.execute();

            statement = con.prepareStatement(deleteAvailability);
            statement.setDate(1, d);
            statement.setString(2, caregiverName);
            statement.execute();

            statement = con.prepareStatement(selectAppointment);
            ResultSet idSet = statement.executeQuery();
            if (!idSet.next()) {
                System.out.println("Please try again");
                return;
            }
            String appointmentId = idSet.getString(1);

            Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
            vaccine.decreaseAvailableDoses(1);

            System.out.println("Appointment ID " + appointmentId + ", Caregiver username " + caregiverName);


        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // showAppointments
        // check 1: check if there is a user currently logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        // check 2: the length for tokens need to be exactly 1 to include all information (with the operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            if (currentCaregiver != null) {
                String selectAppointments = "SELECT A.Id, A.VaccineName, A.Time, A.PatientName FROM Appointments AS A WHERE A.CaregiverName = ? ORDER BY A.Id";
                PreparedStatement statement = con.prepareStatement(selectAppointments);
                statement.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    System.out.println(
                            resultSet.getString(1) + " " +
                                    resultSet.getString(2) + " " +
                                    resultSet.getString(3) + " " +
                                    resultSet.getString(4));
                }
            } else {
                String selectAppointments = "SELECT A.Id, A.VaccineName, A.Time, A.CaregiverName FROM Appointments AS A WHERE A.PatientName = ? ORDER BY A.Id";
                PreparedStatement statement = con.prepareStatement(selectAppointments);
                statement.setString(1, currentPatient.getUsername());
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    System.out.println(
                            resultSet.getString(1) + " " +
                                    resultSet.getString(2) + " " +
                                    resultSet.getString(3) + " " +
                                    resultSet.getString(4));
                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out");
        } else {
            System.out.println("Please login first");
        }
    }
}
