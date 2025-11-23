# 🛒 Shopping Cart – E-Commerce Web Application

A full-stack **E-Commerce Web Application** built using **Spring Boot**, **Thymeleaf**, and **MySQL**.  
Includes both **Admin & User modules**, secure user authentication, product / category management, shopping cart & order processing.

---

## 🚀 Features

### 👤 User Module
- User registration & login with **Spring Security**
- Password encryption using **BCrypt**
- Profile management & image upload
- Browse products with **search, category filters & pagination**
- Add to cart, place orders & track status
- Forgot password & reset functionality via email link

### 🧑‍💼 Admin Module
- Admin dashboard with role‐based access
- Manage categories (CRUD) with image uploads
- Manage products (CRUD) including stock & discount logic
- Order lifecycle management: Processing ➝ Shipped ➝ Delivered ➝ Cancelled
- Email notifications for order status updates

### 🔒 Security & Best Practices
- Role‐based access control (USER/ADMIN)
- CSRF protection, secure file upload validations
- Configuration separated from code (placeholder config)
- Clean architecture (Controller → Service → Repository layers)

---

## 🧰 Tech Stack

| Layer        | Technologies                                         |
|--------------|------------------------------------------------------|
| **Backend**  | Java, Spring Boot, Spring MVC, Spring Security, Spring Data JPA |
| **Frontend** | HTML, CSS, Bootstrap 5, Thymeleaf                    |
| **Database** | MySQL                                                |
| **Tools**    | Maven, Git/GitHub, JavaMailSender, File upload handling |

---

## 📂 Project Structure

src/
├── main/
│ ├── java/com/ecom/
│ │ ├── controller/
│ │ ├── model/
│ │ ├── repository/
│ │ ├── service/
│ │ └── util/
│ └── resources/
│ ├── templates/
│ ├── static/
│ └── application.properties


---

## ⚙️ Configuration

### Database Setup
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecom
spring.datasource.username=root
spring.datasource.password=YOUR_DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL@gmail.com
spring.mail.password=YOUR_APP_PASSWORD
app.upload.dir=uploads

▶️ How to Run the Project

Clone the repository

git clone https://github.com/ChinthaNarendra/Shopping-Cart-.git

Create a MySQL database named ecom

Update src/main/resources/application.properties with correct credentials

Build & run:

mvn spring-boot:run
Visit the application in your browser (default port 8080)

📜 License

This project is provided for educational and portfolio purposes. Use or modify freely.


If you like, I can generate a **Markdown file with badges** (e.g., Java version, build status) and **include screenshot links** from your project.
