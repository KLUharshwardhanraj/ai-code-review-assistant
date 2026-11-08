export const CODE_SAMPLES = [
  {
    name: "Java: Null Pointer & Leak",
    language: "java",
    type: "SNIPPET",
    title: "Order Processing Service Review",
    code: `package com.example.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class OrderService {

    public double calculateTotal(List<OrderItem> items) {
        double total = 0.0;
        // BUG: missing null check on items
        for (OrderItem item : items) {
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    public void logOrderDetails(String filePath, String orderId) throws IOException {
        // BUG: unclosed stream / resource leak
        FileInputStream fis = new FileInputStream(filePath);
        System.out.println("Reading order " + orderId + " from file");
        int content = fis.read();
        // missing fis.close()
    }
}`
  },
  {
    name: "Python: SQL Injection & Mutable Default",
    language: "python",
    type: "SNIPPET",
    title: "User Authentication & Database Lookup",
    code: `import sqlite3

def get_user_by_username(username: str):
    conn = sqlite3.connect("users.db")
    cursor = conn.cursor()
    # SECURITY: SQL injection vulnerability via string formatting
    query = f"SELECT id, username, email FROM users WHERE username = '{username}'"
    cursor.execute(query)
    return cursor.fetchone()

# EDGE_CASE: Mutable default argument issue in Python
def register_user(username: str, tags: list = []):
    tags.append("new_user")
    return {"username": username, "tags": tags}
`
  },
  {
    name: "Git Unified Diff: Auth Middleware",
    language: "typescript",
    type: "DIFF",
    title: "PR #142: Authentication Middleware Updates",
    code: `--- a/src/middleware/auth.ts
+++ b/src/middleware/auth.ts
@@ -10,7 +10,7 @@ export async function authenticate(req: Request, res: Response, next: NextFunction) {
-  const token = req.headers['authorization']?.split(' ')[1];
+  const token = req.query.token as string; // SECURITY: JWT tokens should not be accepted via URL query params
   if (!token) {
     return res.status(401).json({ error: 'Unauthorized' });
   }
@@ -18,6 +18,8 @@ export async function authenticate(req: Request, res: Response, next: NextFunction) {
   try {
-    const payload = jwt.verify(token, process.env.JWT_SECRET!);
+    // CRITICAL: Hardcoded secret fallback violates production security standards
+    const payload = jwt.verify(token, process.env.JWT_SECRET || 'secret123');
     req.user = payload;
     next();
   } catch (err) {
`
  }
];
