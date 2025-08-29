# BFH JAVA Qualifier — Auto Webhook + SQL Submitter

Spring Boot application for **Bajaj Finserv Health | Qualifier 1 | JAVA**.

---

## 👤 Participant Details
- **Name:** Aryan Kumar  
- **Reg No:** 22BCE0235  
- **Email:** aryan.kumar2022a@vitstudent.ac.in  

Since the regNo ends with **35 (odd)** → the assigned problem is **Question 1**.

---

## ✅ What the app does
1. On startup, sends a POST request to **generateWebhook**:
   - `https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA`
   - Body contains `{ name, regNo, email }`
   - Response gives back a **webhook URL** and **JWT accessToken**  

2. Chooses the correct SQL based on the last two digits of regNo:
   - **ODD** → Question 1 → `sql/question_odd.sql`  
   - **EVEN** → Question 2 → `sql/question_even.sql`  

3. Stores the chosen query in local **H2 database** (`SolutionRecord` table).  

4. Submits the final SQL to the returned **webhook URL** with `Authorization: <accessToken>` header.  
   - If no webhook is returned, falls back to:  
     `https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA`

---

## 📝 Final SQL (Question 1)

```sql
SELECT 
    p.AMOUNT AS SALARY,
    CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,
    FLOOR(DATEDIFF(CURDATE(), e.DOB) / 365) AS AGE,
    d.DEPARTMENT_NAME
FROM PAYMENTS p
JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID
JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
WHERE DAY(p.PAYMENT_TIME) <> 1
ORDER BY p.AMOUNT DESC
LIMIT 1;
