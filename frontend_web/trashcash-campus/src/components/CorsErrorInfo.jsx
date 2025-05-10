import React from 'react';

/**
 * A component to explain CORS errors to users or administrators
 */
const CorsErrorInfo = ({ showAdmin = false }) => {
  return (
    <div className="cors-error-info">
      <h3>Image Loading Issues</h3>
      <p>
        Some images can't be loaded due to CORS (Cross-Origin Resource Sharing) restrictions.
        A temporary proxy has been applied as a workaround, but some images may still fail to load.
      </p>
      
      {showAdmin && (
        <div className="admin-info">
          <h4>For Administrators:</h4>
          <p>To fix this issue, you need to configure CORS for your Firebase Storage bucket:</p>
          <ol>
            <li>Go to <a href="https://console.cloud.google.com/storage/browser/trashcashcampusmobile.firebasestorage.app" target="_blank" rel="noopener noreferrer">Google Cloud Console</a></li>
            <li>Navigate to Storage {'>>'} Buckets and select your bucket</li>
            <li>Go to the "Permissions" tab</li>
            <li>Click on "Edit CORS Configuration"</li>
            <li>Add the following configuration:
              <pre>{JSON.stringify([{
                "origin": ["http://localhost:5173", "https://trashcashcampus.web.app"],
                "method": ["GET", "HEAD", "PUT", "POST", "DELETE"],
                "maxAgeSeconds": 3600
              }], null, 2)}</pre>
            </li>
            <li>Click "Save"</li>
          </ol>
          <p>
            <strong>Alternative Method (Command Line):</strong><br/>
            If you have Google Cloud SDK installed, open Command Prompt (not PowerShell) and run:
          </p>
          <pre>gsutil cors set cors.json gs://trashcashcampusmobile.firebasestorage.app</pre>
        </div>
      )}
    </div>
  );
};

export default CorsErrorInfo; 