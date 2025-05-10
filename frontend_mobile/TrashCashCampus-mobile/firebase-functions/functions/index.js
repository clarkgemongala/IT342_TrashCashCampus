const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
admin.initializeApp();

// Configure the email transport using environment variables for security
const transporter = nodemailer.createTransport({
  service: "outlook",
  auth: {
    user: functions.config().email?.user || process.env.EMAIL_USERNAME || "your-service-account@outlook.com",
    pass: functions.config().email?.pass || process.env.EMAIL_PASSWORD || "your-password",
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
        from: `TrashCash Campus <${functions.config().email?.user || process.env.EMAIL_USERNAME || "your-service-account@outlook.com"}>`,
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

// Custom function to send verification emails using Firebase Auth
exports.sendCustomVerificationEmail = functions.https.onCall(async (data, context) => {
  // Check if the request is from an authenticated user
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'The function must be called while authenticated.'
    );
  }

  const userEmail = data.email || context.auth.token.email;
  
  if (!userEmail) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Email is required.'
    );
  }

  try {
    // Generate a custom verification link
    const actionCodeSettings = {
      url: data.continueUrl || 'https://trashcash-campus.netlify.app/emailVerified',
      handleCodeInApp: false,
    };

    // Get the user by email
    const userRecord = await admin.auth().getUserByEmail(userEmail);
    
    // Check if email is already verified
    if (userRecord.emailVerified) {
      return { 
        success: true, 
        message: 'Email is already verified.' 
      };
    }

    // Generate the verification link
    const link = await admin.auth().generateEmailVerificationLink(
      userEmail,
      actionCodeSettings
    );

    // Send custom email using Outlook
    const mailOptions = {
      from: `TrashCash Campus <${functions.config().email?.user || process.env.EMAIL_USERNAME || "your-service-account@outlook.com"}>`,
      to: userEmail,
      subject: "TrashCash Campus - Verify Your Email",
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <h2 style="color: #4CAF50;">TrashCash Campus - Email Verification</h2>
          <p>Thank you for registering with TrashCash Campus! Please verify your email address by clicking the button below:</p>
          <div style="margin: 25px 0;">
            <a href="${link}" style="background-color: #4CAF50; color: white; padding: 12px 20px; text-decoration: none; border-radius: 4px; display: inline-block;">Verify Email</a>
          </div>
          <p>If the button doesn't work, copy and paste this link into your browser:</p>
          <p style="word-break: break-all;"><a href="${link}">${link}</a></p>
          <p>This link will expire in 24 hours.</p>
          <p>If you didn't register for TrashCash Campus, you can ignore this email.</p>
        </div>
      `,
    };

    await transporter.sendMail(mailOptions);
    console.log(`Custom verification email sent to ${userEmail}`);
    
    return { 
      success: true, 
      message: `Verification email sent to ${userEmail}` 
    };
  } catch (error) {
    console.error("Error sending custom verification email:", error);
    throw new functions.https.HttpsError(
      'internal',
      `Error sending verification email: ${error.message}`
    );
  }
});

// When a user is created in Firebase Auth, automatically send a verification email
exports.sendVerificationEmailOnCreate = functions.auth.user().onCreate(async (user) => {
  try {
    if (!user.emailVerified) {
      // Generate custom verification link
      const actionCodeSettings = {
        url: 'https://trashcash-campus.netlify.app/emailVerified',
        handleCodeInApp: false,
      };

      // Generate the verification link
      const link = await admin.auth().generateEmailVerificationLink(
        user.email,
        actionCodeSettings
      );

      // Send custom email using Outlook
      const mailOptions = {
        from: `TrashCash Campus <${functions.config().email?.user || process.env.EMAIL_USERNAME || "your-service-account@outlook.com"}>`,
        to: user.email,
        subject: "TrashCash Campus - Verify Your Email",
        html: `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
            <h2 style="color: #4CAF50;">TrashCash Campus - Email Verification</h2>
            <p>Thank you for registering with TrashCash Campus! Please verify your email address by clicking the button below:</p>
            <div style="margin: 25px 0;">
              <a href="${link}" style="background-color: #4CAF50; color: white; padding: 12px 20px; text-decoration: none; border-radius: 4px; display: inline-block;">Verify Email</a>
            </div>
            <p>If the button doesn't work, copy and paste this link into your browser:</p>
            <p style="word-break: break-all;"><a href="${link}">${link}</a></p>
            <p>This link will expire in 24 hours.</p>
            <p>If you didn't register for TrashCash Campus, you can ignore this email.</p>
          </div>
        `,
      };

      await transporter.sendMail(mailOptions);
      console.log(`Automatic verification email sent to ${user.email}`);
      return { success: true };
    }
    return null;
  } catch (error) {
    console.error("Error sending automatic verification email:", error);
    return { success: false, error: error.message };
  }
});

// This Cloud Function listens for changes to a user's email verification status
// and updates the corresponding Firestore document
exports.syncEmailVerificationStatus = functions.auth
  .user()
  .onUpdate((change, context) => {
    // Get the users before and after the update
    const beforeUser = change.before;
    const afterUser = change.after;

    // Check if the email verification status has changed from false to true
    if (!beforeUser.emailVerified && afterUser.emailVerified) {
      const uid = afterUser.uid;
      console.log(`User ${uid} has verified their email. Updating Firestore...`);

      // Update the Firestore document
      return admin.firestore()
        .collection('users')
        .doc(uid)
        .update({
          isEmailVerified: true,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        })
        .then(() => {
          console.log(`Successfully updated isEmailVerified for user ${uid} in Firestore`);
          return null;
        })
        .catch(error => {
          console.error(`Error updating isEmailVerified for user ${uid}:`, error);
          return null;
        });
    }
    
    // No relevant change, do nothing
    return null;
  });

// Function to check if a user's email is verified
exports.checkEmailVerificationStatus = functions.https.onCall(async (data, context) => {
  const userEmail = data.email;
  const userId = data.userId;
  
  if (!userEmail && !userId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Either email or userId is required.'
    );
  }

  try {
    let userRecord;
    
    // Get user record based on what's provided
    if (userId) {
      userRecord = await admin.auth().getUser(userId);
    } else if (userEmail) {
      userRecord = await admin.auth().getUserByEmail(userEmail);
    }
    
    if (!userRecord) {
      return { 
        isVerified: false,
        message: 'User not found' 
      };
    }
    
    // If verified in Firebase Auth, also ensure Firestore is updated
    if (userRecord.emailVerified) {
      try {
        // Update Firestore record
        await admin.firestore()
          .collection('users')
          .doc(userRecord.uid)
          .update({
            isEmailVerified: true,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
          });
        
        console.log(`Updated isEmailVerified for user ${userRecord.uid} in Firestore via checkEmailVerificationStatus`);
      } catch (error) {
        console.error(`Error updating Firestore from checkEmailVerificationStatus:`, error);
      }
    }
    
    return { 
      isVerified: userRecord.emailVerified,
      userId: userRecord.uid,
      message: userRecord.emailVerified ? 'Email is verified' : 'Email is not verified'
    };
  } catch (error) {
    console.error("Error checking verification status:", error);
    throw new functions.https.HttpsError(
      'internal',
      `Error checking verification status: ${error.message}`
    );
  }
});