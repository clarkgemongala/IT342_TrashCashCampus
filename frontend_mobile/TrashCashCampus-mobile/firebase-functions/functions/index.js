const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
admin.initializeApp();

// Configure the email transport
const transporter = nodemailer.createTransport({
  service: "outlook",
  auth: {
    user: "your-service-account@outlook.com", // Replace with your actual email
    pass: "your-password", // Replace with your actual password
  },
});

exports.sendVerificationEmail = functions.firestore
    .document("verificationCodes/{email}")
    .onCreate(async (snapshot, context) => {
      const emailData = snapshot.data();
      const code = emailData.code;
      const recipientEmail = context.params.email;

      if (!code || !recipientEmail) {
        console.error("Missing required data for email verification");
        return null;
      }

      const mailOptions = {
        from: "TrashCash Campus <your-service-account@outlook.com>",
        to: recipientEmail,
        subject: "Your TrashCash Campus Verification Code",
        html: `
          <div style="font-family: Arial, sans-serif; max-width: 600px;
          margin: 0 auto; padding: 20px;">
            <h2 style="color: #4CAF50;">TrashCash Campus - Email Verification</h2>
            <p>Thank you for registering with TrashCash Campus.
            To complete your registration, please use the code below:</p>
            <div style="background-color: #f5f5f5; padding: 15px;
            text-align: center; font-size: 24px; letter-spacing: 5px;
            font-weight: bold;">
              ${code}
            </div>
            <p style="margin-top: 20px;">This code will expire in 15 minutes.</p>
            <p style="color: #777; font-size: 12px; margin-top: 40px;">
            If you did not request this code, please ignore this email.</p>
          </div>
        `,
      };

      try {
        await transporter.sendMail(mailOptions);
        console.log(`Verification email sent to ${recipientEmail}`);
        return null;
      } catch (error) {
        console.error("Error sending verification email:", error);
        return null;
      }
    });