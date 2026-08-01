# Hands-Free System

Hands-Free is a smart mall shopping system designed to enhance the shopping experience by allowing shoppers to continue shopping without carrying their bags. The system includes an Android mobile application and an admin website that manage shoppers, stores, collectors, pickup requests, and bag storage.

## Project Description

The project addresses the challenge of carrying multiple shopping bags in malls, which can make shopping tiring and inconvenient. Hands-Free allows shoppers to present a QR code to store cashiers, who scan it and add purchased items to the system. The system then assigns the pickup request to a collector, who collects the bags from stores and stores them in designated boxes assigned to each shopper.

## Main Users

- Shopper
- Store cashier
- Collector
- Administrator

## Key Features

- Shopper account creation and login
- QR code generation for shoppers
- Store cashier QR code scanning
- Automatic item assignment to collectors
- Shortest-path routing for collectors
- Item tracking and status updates
- Admin dashboard for managing shoppers, stores, collectors, boxes, and mall map
- Firebase real-time updates across the system

## Technologies Used

- Java for Android application development
- HTML, CSS, and JavaScript for the website
- Firebase Realtime Database for real-time data syncing
- Firebase Authentication for login and account management
- Android Studio for mobile development
- Visual Studio Code for web development

## Algorithms Used

- Nearest Neighbor Algorithm for shortest-path calculation
- Round Robin Load Balancing Algorithm for assigning items to collectors

## System Workflow

1. Shopper creates an account.
2. Admin activates the shopper account and assigns a box.
3. Shopper presents their QR code at the store.
4. Store cashier scans the QR code and adds items.
5. The system assigns the items to a collector.
6. Collector follows the shortest route to collect bags.
7. Collector updates item status after pickup and delivery.
8. Shopper and admin can track item status.

## Testing

The system was tested using:

- Unit testing with JUnit
- Integration testing with 55 successful test scenarios
- Acceptance testing with 15 users over 18 years old
- User feedback collected using a survey similar to WAMMI

## Limitations

- Supports English only
- Android only, no iOS support yet
- Optimized for Android 14 and supports Android 7.0+
- Website tested mainly on Google Chrome and Microsoft Edge
- Not yet fully designed for users with special needs

## Future Work

- Add Arabic language support
- Add shortest-path feature for shoppers
- Make the mall map dynamic
- Support iOS devices
- Improve compatibility across more Android versions
- Add online payment integration
- Display store discounts inside the app

## GitHub

Project link:

https://github.com/Jnoox/Hands-Free-System.git
