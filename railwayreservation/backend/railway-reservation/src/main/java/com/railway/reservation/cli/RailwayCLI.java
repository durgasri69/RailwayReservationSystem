package com.railway.reservation.cli;

import com.railway.reservation.RailwayReservationApplication;
import com.railway.reservation.model.Passenger;
import com.railway.reservation.model.Reservation;
import com.railway.reservation.model.Train;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.PassengerRepository;
import com.railway.reservation.repository.ReservationRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.service.AuthService;
import com.railway.reservation.service.ReservationService;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Reference;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class RailwayCLI implements AutoCloseable {

    private final PassengerRepository passengerRepository;
    private final TrainRepository trainRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final AuthService authService;

    private final Terminal terminal;
    private final LineReader input;

    public RailwayCLI(
            PassengerRepository passengerRepository,
            TrainRepository trainRepository,
            ReservationRepository reservationRepository,
            ReservationService reservationService,
            AuthService authService) throws IOException {

        this.passengerRepository = passengerRepository;
        this.trainRepository = trainRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.authService = authService;
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .jna(true)
                .build();
        this.input = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
    }

    public static void main(String[] args) {

        ConfigurableApplicationContext context =
                new SpringApplicationBuilder(RailwayReservationApplication.class)
                        .web(WebApplicationType.NONE)
                        .run(args);

        RailwayCLI cli = context.getBean(RailwayCLI.class);

        cli.run();

        context.close();
    }

    // =========================================================
    // MAIN LOGIN MENU
    // =========================================================

    public void run() {

        // Create default admin account
        authService.createDefaultAdmin();

        while (true) {

            System.out.println();
            System.out.println("==============================================");
            System.out.println("       RAILWAY RESERVATION SYSTEM");
            System.out.println("==============================================");
            System.out.println("1. User Register");
            System.out.println("2. User Login");
            System.out.println("3. Admin Login");
            System.out.println("4. Exit");
            System.out.println("==============================================");

            System.out.print("Enter your choice: ");

            String choice = readLine();

            switch (choice) {

                case "1":
                    userRegister();
                    break;

                case "2":
                    userLogin();
                    break;

                case "3":
                    adminLogin();
                    break;

                case "4":
                    System.out.println();
                    System.out.println(
                            "Thank you for using Railway Reservation System!"
                    );
                    return;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // =========================================================
    // USER REGISTRATION
    // =========================================================
private void userRegister() {

        System.out.println();
        System.out.println("========== USER REGISTRATION ==========");

        System.out.print("Enter Username: ");
        String username = readLine().trim();

        if (username.isBlank()) {
            System.out.println("Username cannot be empty!");
            return;
        }

        String password = readPasswordInput("Enter Password: ");

        if (!isValidPassword(password)) {
            System.out.println();
            System.out.println("Invalid password!");
            System.out.println("Password must contain:");
            System.out.println("- Minimum 8 characters");
            System.out.println("- At least one letter");
            System.out.println("- At least one number");
            System.out.println("- At least one special character");
            return;
        }

        User user =
                authService.register(
                        username,
                        password,
                        "USER"
                );

        if (user == null) {

            System.out.println();
            System.out.println("Username already exists!");

        } else {

            System.out.println();
            System.out.println("User registered successfully!");
            System.out.println("----------------------------------");
            System.out.println("User ID  : " + user.getUserId());
            System.out.println("Username : " + user.getUsername());
            System.out.println("Role     : " + user.getRole());
            System.out.println("----------------------------------");
            System.out.println("Please remember your User ID.");
        }
    }

    // =========================================================
    // PASSWORD INPUT AND VALIDATION
    // =========================================================

    private String readLine() {
        return input.readLine();
    }

    private String readPasswordInput(String prompt) {
        boolean[] visible = {false};
        String widgetName = "toggle-password-visibility";
        String keySequence = KeyMap.ctrl('R');
        KeyMap<Binding> keyMap = input.getKeyMaps().get(LineReader.MAIN);
        Binding previousBinding = keyMap.getBound(keySequence);

        Widget toggleVisibility = () -> {
            visible[0] = !visible[0];
            input.callWidget(LineReader.REDRAW_LINE);
            input.callWidget(LineReader.REDISPLAY);
            return true;
        };

        input.getWidgets().put(widgetName, toggleVisibility);
        keyMap.bind(new Reference(widgetName), keySequence);

        try {
            return input.readLine(
                    prompt + " (Ctrl+R toggles visibility): ",
                    null,
                    new MaskingCallback() {
                        @Override
                        public String display(String buffer) {
                            return visible[0] ? buffer : "*".repeat(buffer.length());
                        }

                        @Override
                        public String history(String buffer) {
                            return "";
                        }
                    },
                    null
            );
        } finally {
            if (previousBinding == null) {
                keyMap.unbind(keySequence);
            } else {
                keyMap.bind(previousBinding, keySequence);
            }
            input.getWidgets().remove(widgetName);
        }
    }

    @Override
    public void close() throws IOException {
        terminal.close();
    }

    private boolean isValidPassword(String password) {

        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasLetter = false;
        boolean hasNumber = false;
        boolean hasSpecialCharacter = false;

        for (char ch : password.toCharArray()) {

            if (Character.isLetter(ch)) {
                hasLetter = true;
            } else if (Character.isDigit(ch)) {
                hasNumber = true;
            } else if (!Character.isWhitespace(ch)) {
                hasSpecialCharacter = true;
            }
        }

        return hasLetter && hasNumber && hasSpecialCharacter;
    }

    // =========================================================
    // USER LOGIN
    // =========================================================

    private void userLogin() {

        System.out.println();
        System.out.println("========== USER LOGIN ==========");

        System.out.print("Username: ");
        String username = readLine();

        String password = readPasswordInput("Password: ");

        User user =
                authService.login(username, password);

        if (user == null ||
                !user.getRole().equals("USER")) {

            System.out.println();
            System.out.println("Invalid user credentials!");

            return;
        }

        System.out.println();
        System.out.println("Login successful!");
        System.out.println(
                "Welcome, " + user.getUsername()
        );

        userMenu();
    }

    // =========================================================
    // ADMIN LOGIN
    // =========================================================

    private void adminLogin() {

        System.out.println();
        System.out.println("========== ADMIN LOGIN ==========");

        System.out.print("Username: ");
        String username = readLine();

        String password = readPasswordInput("Password: ");

        User user =
                authService.login(username, password);

        if (user == null ||
                !user.getRole().equals("ADMIN")) {

            System.out.println();
            System.out.println("Invalid admin credentials!");

            return;
        }

        System.out.println();
        System.out.println("Admin login successful!");
        System.out.println(
                "Welcome, " + user.getUsername()
        );

        adminMenu();
    }

    // =========================================================
    // USER MENU
    // =========================================================

    private void userMenu() {

        while (true) {

            System.out.println();
            System.out.println("==============================================");
            System.out.println("                USER MENU");
            System.out.println("==============================================");
            System.out.println("1. Reserve Seat");
            System.out.println("2. Cancel Ticket");
            System.out.println("3. Search PNR");
            System.out.println("4. Seat Availability & Waiting List");
            System.out.println("5. Logout");
            System.out.println("==============================================");

            System.out.print("Enter your choice: ");

            String choice = readLine();

            switch (choice) {

                case "1":
                    reserveSeat();
                    break;

                case "2":
                    cancelTicket();
                    break;

                case "3":
                    searchPNR();
                    break;

                case "4":
                    seatAvailabilityAndWaitingList();
                    break;

                case "5":
                    System.out.println();
                    System.out.println(
                            "User logged out successfully."
                    );
                    return;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // =========================================================
    // ADMIN MENU
    // =========================================================

    private void adminMenu() {

        while (true) {

            System.out.println();
            System.out.println("==============================================");
            System.out.println("                ADMIN MENU");
            System.out.println("==============================================");
            System.out.println("1. Register Train");
            System.out.println("2. Add Passenger");
            System.out.println("3. Reservation Report");
            System.out.println("4. Seat Availability & Waiting List");
            System.out.println("5. Logout");
            System.out.println("==============================================");

            System.out.print("Enter your choice: ");

            String choice = readLine();

            switch (choice) {

                case "1":
                    registerTrain();
                    break;

                case "2":
                    addPassenger();
                    break;

                case "3":
                    reservationReport();
                    break;

                case "4":
                    seatAvailabilityAndWaitingList();
                    break;

                case "5":
                    System.out.println();
                    System.out.println(
                            "Admin logged out successfully."
                    );
                    return;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // =========================================================
    // REGISTER TRAIN
    // =========================================================

    private void registerTrain() {

        System.out.println();
        System.out.println("---------- REGISTER TRAIN ----------");

        System.out.print("Enter Train Number: ");
        String trainNumber = readLine();

        Train existingTrain =
                trainRepository.findByTrainNumber(trainNumber);

        if (existingTrain != null) {

            System.out.println(
                    "Train number already exists!"
            );

            return;
        }

        System.out.print("Enter Train Name: ");
        String trainName = readLine();

        System.out.print("Enter Source: ");
        String source = readLine();

        System.out.print("Enter Destination: ");
        String destination = readLine();

        System.out.print("Enter Total Seats: ");

        int totalSeats;

        try {

            totalSeats =
                    Integer.parseInt(readLine());

        } catch (NumberFormatException e) {

            System.out.println(
                    "Invalid number of seats!"
            );

            return;
        }

        if (totalSeats <= 0) {

            System.out.println(
                    "Seats must be greater than 0!"
            );

            return;
        }

        Train train = new Train();

        train.setTrainNumber(trainNumber);
        train.setTrainName(trainName);
        train.setSource(source);
        train.setDestination(destination);
        train.setTotalSeats(totalSeats);
        train.setAvailableSeats(totalSeats);

        Train savedTrain =
                trainRepository.save(train);

        System.out.println();
        System.out.println(
                "Train registered successfully!"
        );

        System.out.println("----------------------------------");
        System.out.println(
                "Train ID       : "
                        + savedTrain.getTrainId()
        );
        System.out.println(
                "Train Number   : "
                        + savedTrain.getTrainNumber()
        );
        System.out.println(
                "Train Name     : "
                        + savedTrain.getTrainName()
        );
        System.out.println(
                "Source         : "
                        + savedTrain.getSource()
        );
        System.out.println(
                "Destination    : "
                        + savedTrain.getDestination()
        );
        System.out.println(
                "Total Seats    : "
                        + savedTrain.getTotalSeats()
        );
        System.out.println(
                "Available Seats: "
                        + savedTrain.getAvailableSeats()
        );
        System.out.println("----------------------------------");
    }

    // =========================================================
    // ADD PASSENGER
    // =========================================================

    private void addPassenger() {

        System.out.println();
        System.out.println("---------- ADD PASSENGER ----------");

        String name = readPassengerName();
        int age = readPassengerAge();
        String gender = readPassengerGender();
        String category = readPassengerCategory();
        String phone = readPassengerPhone();
        String email = readPassengerGmail();

        Passenger passenger =
                new Passenger(
                        name,
                        age,
                        gender,
                        category,
                        phone,
                        email
                );

        Passenger savedPassenger =
                passengerRepository.save(passenger);

        System.out.println();
        System.out.println(
                "Passenger added successfully!"
        );

        System.out.println("----------------------------------");
        System.out.println(
                "Passenger ID : "
                        + savedPassenger.getPassengerId()
        );
        System.out.println(
                "Name         : "
                        + savedPassenger.getName()
        );
        System.out.println(
                "Age          : "
                        + savedPassenger.getAge()
        );
        System.out.println(
                "Gender       : "
                        + savedPassenger.getGender()
        );
        System.out.println(
                "Category     : "
                        + savedPassenger.getCategory()
        );
        System.out.println(
                "Phone        : "
                        + savedPassenger.getPhone()
        );
        System.out.println(
                "Email        : "
                        + savedPassenger.getEmail()
        );
        System.out.println(
                "PNR          : "
                        + savedPassenger.getPnr()
        );
        System.out.println("----------------------------------");
    }

    private String readPassengerName() {
        while (true) {
            System.out.print("Enter Name: ");
            String name = readLine().trim();
            if (name.matches("[A-Za-z ]+") && !name.isBlank()) {
                return name;
            }
            System.out.println("Invalid name! Enter letters only.");
        }
    }

    private int readPassengerAge() {
        while (true) {
            System.out.print("Enter Age (1-120): ");
            String ageInput = readLine();
            if (ageInput.matches("[0-9]+")) {
                try {
                    int age = Integer.parseInt(ageInput);
                    if (age >= 1 && age <= 120) {
                        return age;
                    }
                } catch (NumberFormatException ignored) {
                    // An all-digit value larger than int is outside the valid age range.
                }
            }
            System.out.println("Invalid age! Enter a number from 1 to 120.");
        }
    }

    private String readPassengerGender() {
        while (true) {
            System.out.println("========== SELECT GENDER ==========");
            System.out.println("1. Male");
            System.out.println("2. Female");
            System.out.println("3. Other");
            System.out.println();
            System.out.print("Enter your choice: ");
            switch (readLine()) {
                case "1":
                    return "Male";
                case "2":
                    return "Female";
                case "3":
                    return "Other";
                default:
                    System.out.println("Invalid choice! Please select 1, 2, or 3.");
            }
        }
    }

    private String readPassengerCategory() {
        while (true) {
            System.out.println();
            System.out.println("Select Category:");
            System.out.println("1. General");
            System.out.println("2. Senior Citizen");
            System.out.println("3. Women");
            System.out.println("4. Divyang");
            System.out.print("Enter Category Choice: ");
            switch (readLine()) {
                case "1":
                    return "General";
                case "2":
                    return "Senior Citizen";
                case "3":
                    return "Women";
                case "4":
                    return "Divyang";
                default:
                    System.out.println("Invalid choice! Please select 1, 2, 3, or 4.");
            }
        }
    }

    private String readPassengerPhone() {
        while (true) {
            System.out.print("Enter 10-digit Indian mobile number: ");
            String phone = readLine();
            if (phone.matches("[0-9]{10}")) {
                return phone;
            }
            System.out.println("Invalid phone number! Enter exactly 10 digits.");
        }
    }

    private String readPassengerGmail() {
        while (true) {
            System.out.print("Enter Gmail address: ");
            String email = readLine();
            if (email.matches("[A-Za-z0-9]+(?:\\.[A-Za-z0-9]+)*(?:\\+[A-Za-z0-9]+(?:\\.[A-Za-z0-9]+)*)?@gmail\\.com")) {
                return email;
            }
            System.out.println("Invalid Gmail address! Enter a valid @gmail.com address.");
        }
    }

    // =========================================================
    // RESERVE SEAT
    // =========================================================

    private void reserveSeat() {

        System.out.println();
        System.out.println("========== RESERVE SEAT ==========");

        // Show all registered trains before asking the passenger to choose.
        List<Train> trains = trainRepository.findAll();

        if (trains.isEmpty()) {
            System.out.println("No trains available.");
            return;
        }

        bookingFlow:
        while (true) {
        Long trainId;
        Train train;
        int availableSeats;

        while (true) {
            System.out.println();
            System.out.println("================ AVAILABLE TRAINS ================");
            System.out.printf("%-8s | %-10s | %-24s | %-16s | %-16s | %-12s | %-15s%n",
                    "Train ID", "Train No", "Train Name", "Source", "Destination",
                    "Total Seats", "Available Seats");
            System.out.println("---------------------------------------------------------------------------------------------------------------");

            for (Train listedTrain : trains) {
                int listedAvailableSeats =
                        reservationService.getAvailableSeatCount(listedTrain.getTrainId());
                System.out.printf("%-8d | %-10s | %-24s | %-16s | %-16s | %-12d | %-15d%n",
                        listedTrain.getTrainId(),
                        listedTrain.getTrainNumber(),
                        listedTrain.getTrainName(),
                        listedTrain.getSource(),
                        listedTrain.getDestination(),
                        listedTrain.getTotalSeats(),
                        listedAvailableSeats);
            }

            System.out.println("---------------------------------------------------------------------------------------------------------------");
            System.out.print("Enter Train ID: ");

            try {
                trainId = Long.parseLong(readLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid Train ID!");
                continue;
            }

            train = trainRepository.findById(trainId).orElse(null);

            if (train == null) {
                System.out.println("Train not found!");
                continue;
            }

            availableSeats = reservationService.getAvailableSeatCount(trainId);
            break;
        }

        System.out.println();
        System.out.println("========== SELECTED TRAIN ==========");
        System.out.println("Train Number    : " + train.getTrainNumber());
        System.out.println("Train Name      : " + train.getTrainName());
        System.out.println("Source          : " + train.getSource());
        System.out.println("Destination     : " + train.getDestination());
        System.out.println("Total Seats     : " + train.getTotalSeats());
        System.out.println("Available Seats : " + availableSeats);
        System.out.println("====================================");

        int requestedQuantity = readPositiveInteger("How many seats do you want to book? ");
        availableSeats = reservationService.getAvailableSeatCount(trainId);
        int bookingQuantity;
        boolean joinWaitingList = false;

        System.out.println("Seats Requested: " + requestedQuantity);
        System.out.println("Seats Available: " + availableSeats);

        if (availableSeats >= requestedQuantity) {
            System.out.println("All " + requestedQuantity + " seats are available.");
            if (!readYesNo("Continue booking? (Y/N): ")) {
                return;
            }
            bookingQuantity = requestedQuantity;
        } else if (availableSeats > 0) {
            System.out.println("Only " + availableSeats + " seats are available.");
            if (!readYesNo("Would you like to book only " + availableSeats + " seats? (Y/N): ")) {
                return;
            }
            bookingQuantity = availableSeats;
        } else {
            System.out.println("No seats are currently available.");
            if (!readYesNo(
                    "Would you like to join the waiting list for "
                            + requestedQuantity + " passengers? (Y/N): ")) {
                if (readYesNo("Would you like to choose a different train? (Y/N): ")) {
                    continue bookingFlow;
                }
                return;
            }
            bookingQuantity = requestedQuantity;
            joinWaitingList = true;
        }

        System.out.println();
        System.out.println("Enter one contact number and Gmail address for all tickets.");
        String sharedPhone = readPassengerPhone();
        String sharedEmail = readPassengerGmail();

        List<Passenger> passengers = new ArrayList<>();
        for (int i = 1; i <= bookingQuantity; i++) {
            System.out.println();
            System.out.println("========== PASSENGER " + i + " ==========");
            passengers.add(readPassengerDetails(sharedPhone, sharedEmail));
        }

        while (true) {
            try {
                ReservationService.BookingBatchResult result =
                        reservationService.bookTickets(
                                passengers.subList(0, bookingQuantity),
                                trainId,
                                joinWaitingList
                        );

                if (!result.reservations().isEmpty()) {
                    displayBookingConfirmations(result.reservations(), train);
                    return;
                }

                availableSeats = result.availableSeats();
                System.out.println();
                System.out.println("Availability changed while entering passenger details.");
                System.out.println("Updated available seats: " + availableSeats);

                if (availableSeats == 0) {
                    joinWaitingList = readYesNo(
                            "No seats are available. Join the waiting list for "
                                    + bookingQuantity + " passengers? (Y/N): ");
                    if (!joinWaitingList) {
                        if (readYesNo("Would you like to choose a different train? (Y/N): ")) {
                            continue bookingFlow;
                        }
                        return;
                    }
                } else {
                    int reducedQuantity = Math.min(availableSeats, bookingQuantity);
                    if (!readYesNo(
                            "Only " + availableSeats + " seats are now available. "
                                    + "Book " + reducedQuantity + " seats? (Y/N): ")) {
                        return;
                    }
                    bookingQuantity = reducedQuantity;
                    joinWaitingList = false;
                }
            } catch (Exception e) {
                System.out.println();
                System.out.println("Reservation failed!");
                System.out.println("Reason: " + e.getMessage());
                return;
            }
        }
        }
    }

    private Passenger readPassengerDetails(String phone, String email) {
        String name = readPassengerName();
        int age = readPassengerAge();
        String gender = readPassengerGender();
        String category = readPassengerCategory();
        return new Passenger(name, age, gender, category, phone, email);
    }

    private int readPositiveInteger(String prompt) {
        while (true) {
            System.out.print(prompt);
            String inputValue = readLine();
            if (inputValue.matches("[0-9]+")) {
                try {
                    int value = Integer.parseInt(inputValue);
                    if (value > 0) {
                        return value;
                    }
                } catch (NumberFormatException ignored) {
                    // Values too large for an int are invalid quantities.
                }
            }
            System.out.println("Invalid quantity! Enter a positive whole number.");
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            switch (readLine().trim().toUpperCase()) {
                case "Y":
                    return true;
                case "N":
                    return false;
                default:
                    System.out.println("Please enter Y or N.");
            }
        }
    }

    private void displayBookingConfirmations(List<Reservation> reservations, Train train) {
        System.out.println();
        System.out.println("Tickets booked: " + reservations.size());
        for (Reservation reservation : reservations) {
            Passenger passenger = reservation.getPassenger();
            System.out.println("----------------------------------");
            System.out.println("Passenger    : " + passenger.getName());
            System.out.println("Age          : " + passenger.getAge());
            System.out.println("Gender       : " + passenger.getGender());
            System.out.println("Category     : " + passenger.getCategory());
            System.out.println("Phone        : " + passenger.getPhone());
            System.out.println("Email        : " + passenger.getEmail());
            System.out.println("Train        : " + train.getTrainName());
            System.out.println("Train Number : " + train.getTrainNumber());
            System.out.println("PNR          : " + reservation.getPnr());
            if (reservation.getSeatNumber() != null) {
                System.out.println("Seat Number  : " + reservation.getSeatNumber());
            }
            System.out.println("Status       : " + reservation.getStatus());
        }
        System.out.println("----------------------------------");
    }

    // =========================================================
    // CANCEL TICKET
    // =========================================================

    private void cancelTicket() {

        System.out.println();
        System.out.println(
                "---------- CANCEL TICKET ----------"
        );

        while (true) {
            System.out.print("Enter PNR (or 0 to return): ");
            String pnr = readLine().trim();
            if ("0".equals(pnr)) {
                return;
            }

            Reservation reservation = reservationService.searchPNR(pnr);
            if (reservation == null) {
                System.out.println("Invalid PNR! Reservation not found.");
                if (readYesNo("Would you like to try another PNR? (Y/N): ")) {
                    continue;
                }
                return;
            }

            Train train = reservation.getTrain();
            Passenger passenger = reservation.getPassenger();
            System.out.println();
            System.out.println("========== RESERVATION DETAILS ==========");
            System.out.println("PNR          : " + reservation.getPnr());
            System.out.println("Passenger    : "
                    + (passenger == null ? "N/A" : passenger.getName()));
            System.out.println("Train ID     : " + train.getTrainId());
            System.out.println("Train Number : " + train.getTrainNumber());
            System.out.println("Train Name   : " + train.getTrainName());
            System.out.println("Source       : " + train.getSource());
            System.out.println("Destination  : " + train.getDestination());
            if (reservation.getSeatNumber() != null) {
                System.out.println("Seat Number  : " + reservation.getSeatNumber());
            }
            System.out.println("Status       : " + reservation.getStatus());
            System.out.println("==========================================");

            if (!readYesNo("Cancel this reservation? (Y/N): ")) {
                System.out.println("Reservation was not changed.");
                return;
            }

            try {
                if (reservationService.cancelTicket(pnr)) {
                    System.out.println("Ticket cancelled successfully!");
                    System.out.println("PNR: " + pnr);
                    if (reservation.getSeatNumber() != null) {
                        Reservation allocatedReservation =
                                reservationService.getAllReservations().stream()
                                        .filter(candidate -> !pnr.equals(candidate.getPnr()))
                                        .filter(candidate -> "CONFIRMED".equals(candidate.getStatus()))
                                        .filter(candidate -> candidate.getTrain().getTrainId()
                                                .equals(train.getTrainId()))
                                        .filter(candidate -> reservation.getSeatNumber()
                                                .equals(candidate.getSeatNumber()))
                                        .findFirst()
                                        .orElse(null);
                        if (allocatedReservation != null) {
                            System.out.println(
                                    "Released seat allocated to the next waiting passenger."
                            );
                            System.out.println(
                                    "New PNR: " + allocatedReservation.getPnr()
                            );
                        }
                    }
                } else {
                    System.out.println("Ticket could not be cancelled; it may already be cancelled.");
                }
            } catch (Exception e) {
                System.out.println("Cancellation failed!");
                System.out.println("Reason: " + e.getMessage());
            }
            return;
        }
    }

    // =========================================================
    // SEARCH PNR
    // =========================================================

    private void searchPNR() {

        System.out.println();
        System.out.println(
                "---------- SEARCH PNR ----------"
        );

        System.out.print("Enter PNR: ");

        String pnr = readLine();

        try {

            Reservation reservation =
                    reservationService
                            .searchPNR(pnr);

            if (reservation == null) {

                System.out.println(
                        "PNR not found!"
                );

                return;
            }

            System.out.println();
            System.out.println(
                    "----------------------------------"
            );

            System.out.println(
                    "PNR          : "
                            + reservation.getPnr()
            );

            if (reservation.getPassenger() != null) {

                System.out.println(
                        "Passenger    : "
                                + reservation
                                .getPassenger()
                                .getName()
                );
            }

            if (reservation.getTrain() != null) {

                System.out.println(
                        "Train        : "
                                + reservation
                                .getTrain()
                                .getTrainName()
                );

                System.out.println(
                        "Train Number : "
                                + reservation
                                .getTrain()
                                .getTrainNumber()
                );
            }

            System.out.println(
                    "Seat Number  : "
                            + reservation.getSeatNumber()
            );

            System.out.println(
                    "Status       : "
                            + reservation.getStatus()
            );

            System.out.println(
                    "----------------------------------"
            );

        } catch (Exception e) {

            System.out.println(
                    "Unable to search PNR!"
            );

            System.out.println(
                    "Reason: " + e.getMessage()
            );
        }
    }

    // =========================================================
    // SEAT AVAILABILITY
    // =========================================================

    private void seatAvailabilityAndWaitingList() {

        System.out.println();
        System.out.println("========== SEAT AVAILABILITY & WAITING LIST ==========");

        List<Train> trains = trainRepository.findAll();

        if (trains.isEmpty()) {
            System.out.println("No trains available.");
            return;
        }

        System.out.println();
        System.out.println("========== AVAILABLE TRAINS ==========");
        System.out.printf("%-10s %-15s %-25s %-18s %-18s%n",
                "Train ID", "Train Number", "Train Name", "Source", "Destination");
        System.out.println("--------------------------------------------------------------------------------");

        for (Train train : trains) {
            System.out.printf("%-10d %-15s %-25s %-18s %-18s%n",
                    train.getTrainId(),
                    train.getTrainNumber(),
                    train.getTrainName(),
                    train.getSource(),
                    train.getDestination());
        }

        System.out.println("--------------------------------------------------------------------------------");
        System.out.print("Enter Train ID: ");

        Long trainId;

        try {
            trainId = Long.parseLong(readLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid Train ID!");
            return;
        }

        Train train = trainRepository.findById(trainId).orElse(null);

        if (train == null) {
            System.out.println("Train not found!");
            return;
        }

        System.out.println();
        System.out.println("========== TRAIN STATUS ==========");
        System.out.println("Train ID         : " + train.getTrainId());
        System.out.println("Train Number     : " + train.getTrainNumber());
        System.out.println("Train Name       : " + train.getTrainName());
        System.out.println("Source           : " + train.getSource());
        System.out.println("Destination      : " + train.getDestination());
        System.out.println("Total Seats      : " + train.getTotalSeats());
        System.out.println("Available Seats  : " + train.getAvailableSeats());

        System.out.println();
        System.out.println("========== WAITING LIST ==========");

        try {
            int count = reservationService.getWaitingCount(trainId);

            System.out.println("Total Waiting    : " + count);

            if (count > 0) {
                System.out.println();
                System.out.println("Category-wise waiting count:");
                System.out.println("Category counts can be added after the waiting-list service exposes category-wise data.");
            }

        } catch (Exception e) {
            System.out.println("Unable to display waiting list!");
            System.out.println("Reason: " + e.getMessage());
        }

        System.out.println("==================================");
    }



    // =========================================================
    // RESERVATION REPORT
    // =========================================================

    private void reservationReport() {

        System.out.println();
        System.out.println(
                "========== RESERVATION REPORT =========="
        );

        List<Reservation> reservations =
                reservationRepository.findAll();

        if (reservations.isEmpty()) {

            System.out.println(
                    "No reservations found."
            );

            return;
        }

        for (Reservation reservation :
                reservations) {

            System.out.println(
                    "----------------------------------------"
            );

            System.out.println(
                    "PNR          : "
                            + reservation.getPnr()
            );

            if (reservation.getPassenger() != null) {

                System.out.println(
                        "Passenger    : "
                                + reservation
                                .getPassenger()
                                .getName()
                );
            }

            if (reservation.getTrain() != null) {

                System.out.println(
                        "Train        : "
                                + reservation
                                .getTrain()
                                .getTrainName()
                );

                System.out.println(
                        "Train Number : "
                                + reservation
                                .getTrain()
                                .getTrainNumber()
                );
            }

            System.out.println(
                    "Seat Number  : "
                            + reservation.getSeatNumber()
            );

            System.out.println(
                    "Status       : "
                            + reservation.getStatus()
            );
        }

        System.out.println(
                "----------------------------------------"
        );

        System.out.println(
                "Total Reservations: "
                        + reservations.size()
        );

        System.out.println(
                "========================================"
        );
    }
}