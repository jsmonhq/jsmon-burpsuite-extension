# JSMon Burp Suite Extension

A powerful Burp Suite extension that automatically scans JavaScript files using the JSMon API. Discover secrets, API endpoints, domains, and other intelligence from JavaScript files as you browse.

## Features

- 🔑 **Easy API Key Configuration** - Enter your JSMon API key directly in the extension
- 📁 **Workspace Management** - Fetch existing workspaces or create new ones
- 🌐 **Domain Scoping** - Limit scanning to specific domains (includes subdomains)
- ⚡ **Automatic Scanning** - Automatically scan JS files as you browse
- 📊 **Intelligence Dashboard** - View secrets, JS URLs, API paths, domains, IPs, emails, S3 buckets, and more
- 📋 **Copy to Clipboard** - One-click copy for all intelligence data
- 🔄 **Real-time Updates** - See scan results and counts update in real-time
- 🎨 **Dark/Light Theme Support** - Automatically adapts to Burp Suite's theme

## Requirements

- Burp Suite Professional or Community Edition (2024.1 or later)
- Java 11 or higher
- JSMon API account and API key

## Installation

1. Download the latest `jsmon-burp-extension-1.0.1.jar` file
2. Open Burp Suite
3. Go to the **Extensions** tab
4. Click the **Add** button
5. Select **Extension type**: Java
6. Click **Select file** and choose the downloaded JAR file
7. Verify the extension loads without errors in the **Output** tab
8. You should see a new **JSMon** tab in Burp Suite

## Quick Start Guide

### Step 1: Configure API Key

1. Open the **JSMon** tab in Burp Suite
2. Enter your JSMon API key in the "🔑 API Key" field
3. Click **Fetch Workspaces** to retrieve your workspaces
4. Your user profile (name, email, JSScan credits) will be displayed automatically

### Step 2: Select or Create Workspace

- **Select Existing Workspace**: Choose a workspace from the dropdown
- **Create New Workspace**: Enter a name and click **Create**

### Step 3: Configure Domain Scoping (Optional)

- Enter domain(s) in the "🌐 Domain Scoping" field (one per line or comma-separated)
- Leave empty to scan all JS files regardless of domain
- Subdomains are automatically included (e.g., `example.com` includes `sub.example.com`)

### Step 4: Enable Automatic Scanning

- Check **Enable Automatic Scanning** to automatically scan JS files as you browse
- When enabled, the extension will:
  - Scan all existing JS files in Burp's history
  - Automatically process new JS files as you browse
  - Respect your domain scoping settings

### Step 5: View Intelligence Data

- Click on the **📊 JS-Intelligence Data** tab to view:
  - 🔐 **Secrets** - API keys, tokens, and other secrets found
  - 🔗 **JS URLs** - All JavaScript files discovered
  - 🛣️ **API Paths** - API endpoints extracted from JS files
  - 🔗 **URLs** - All URLs found in JavaScript
  - 🌐 **Domains** - Domains discovered
  - 🌐 **IP Addresses** - IP addresses found
  - 📧 **Emails** - Email addresses discovered
  - 🪣 **S3 Buckets** - S3 bucket names found
  - 📦 **Invalid Node Modules** - NPM confusion packages detected

## Usage

### Automatic Scanning

When automatic scanning is enabled:
- The extension monitors all HTTP responses in Burp Suite
- JavaScript files are automatically detected (by `.js` extension or `Content-Type` header)
- Files are sent to JSMon API with all relevant headers (User-Agent, Cookie, Authorization, etc.)
- Each URL is processed only once to avoid duplicates
- Results appear in real-time in the intelligence tabs

### Manual Scanning

1. Click **🚀 Start Manual Scan** to scan all JS files from Burp's HTTP history
2. Progress is shown in the status log
3. Results are automatically displayed in the intelligence tabs

### Viewing Intelligence Data

- **Secrets Tab**: View all secrets found (API keys, tokens, etc.)
- **JS Intelligence Tabs**: Browse through different types of intelligence data
- **Counts**: Each tab shows the total count in parentheses (e.g., "📧 Emails (188)")
- **Pagination**: Use **◀ Prev** and **Next ▶** buttons to navigate through pages
- **Copy All**: Click **📋 Copy All** button to copy all data from a tab to clipboard

### Copying Data

- **Copy Selected Cells**: Select cells in any table and press `Ctrl+C` (or `Cmd+C` on Mac)
- **Copy All Data**: Click the **📋 Copy All** button at the bottom of each intelligence tab
  - This copies all values from the first column (the actual data, not timestamps)
  - Data is copied one item per line, ready to paste anywhere

## Configuration Options

| Option | Description |
|--------|-------------|
| **API Key** | Your JSMon API key (required) |
| **Workspace** | The workspace where scan results will be stored (required) |
| **Scoped Domain** | Domain(s) to limit scanning scope. Leave empty to scan all domains |
| **Automatic Scanning** | Enable/disable automatic scanning of JS files |

## Understanding the Intelligence Data

### Secrets
API keys, tokens, passwords, and other sensitive information found in JavaScript files.

### JS URLs
All JavaScript file URLs that have been scanned. Useful for identifying all JS files in scope.

### API Paths
API endpoints and paths extracted from JavaScript files. Great for discovering hidden endpoints.

### URLs
All URLs found in JavaScript files, including internal and external links.

### Domains
All domains discovered in JavaScript files. Helps identify all domains used by the application.

### IP Addresses
IP addresses found in JavaScript files, including internal and external IPs.

### Emails
Email addresses discovered in JavaScript files. Useful for identifying contacts and user emails.

### S3 Buckets
Amazon S3 bucket names found in JavaScript files. Can reveal misconfigured or exposed buckets.

### Invalid Node Modules
NPM confusion packages detected - packages with typosquatting or suspicious names.

## Status Log

The status log at the bottom shows:
- Configuration changes
- Scan progress
- API responses
- Errors and warnings
- Copy operations

## Troubleshooting

### Extension Not Loading
- Ensure you're using Burp Suite 2024.1 or later
- Check that Java 11+ is installed
- Verify the JAR file is not corrupted
- Check the **Output** tab in Burp Suite for error messages

### Workspaces Not Fetching
- Verify your API key is correct
- Check your internet connection
- Look for error messages in the status log

### JS Files Not Being Scanned
- Ensure **Automatic Scanning** is enabled
- Check that the domain matches your scoped domain (if set)
- Verify the workspace is selected
- Check the status log for any error messages

### Counts Showing Zero
- Counts are fetched automatically when you select a workspace
- If counts are zero, try:
  - Selecting the workspace again
  - Clicking on the intelligence tabs to refresh data
  - Checking if there's actual data in your workspace

### Copy Not Working
- Ensure you have data loaded in the table
- Try selecting cells first, then copying
- Use the **Copy All** button for bulk copying

## Tips & Best Practices

1. **Start with Domain Scoping**: Limit your scans to your target domain to avoid scanning unrelated JS files
2. **Monitor JSScan Credits**: Check your remaining credits in the User section
3. **Use Manual Scan First**: Run a manual scan to see what data is available before enabling automatic scanning
4. **Export Data Regularly**: Use the Copy All buttons to export data for further analysis
5. **Check Secrets First**: Always review the Secrets tab first as it contains the most critical findings

## Version

**Current Version**: 1.1.0

## Support

For issues, questions, or feature requests, please check the extension's status log for detailed error messages.

## License

This extension is provided as-is for use with JSMon API.
