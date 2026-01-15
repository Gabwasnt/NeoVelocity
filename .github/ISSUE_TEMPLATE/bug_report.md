---
name: Bug Report
about: Create a report to help us improve NeoVelocity
title: ''
labels: bug, unverified
assignees: ''
---

### Pre-Submission Checks
<!-- Please check the following boxes before submitting. -->
*   [ ] I am running the latest version of **NeoVelocity**.
*   [ ] I have updated **Velocity Proxy** to the latest build.
*   [ ] I have updated **NeoForge** to the latest version available for my Minecraft version.

---

### Description
<!-- Briefly describe the issue. Is it a crash, a disconnect, or a visual bug? -->


### Steps to Reproduce
<!-- How can we make this happen on our end? -->
1.
2.
3.

**Expected behavior:**
**Actual behavior:**

---

### Environment
<!-- Please verify the exact versions below. "Latest" is not a version number. -->
*   **Minecraft Version:**
*   **NeoVelocity Version:**
*   **Velocity Proxy Version:**
*   **Hosting Platform:** (e.g., Pterodactyl, Docker, Localhost)

### Logs (Required)
<!-- Please upload your logs to https://mclo.gs/ and paste the links below. -->
<!-- WE NEED BOTH LOGS TO DIAGNOSE CONNECTION ISSUES. -->

*   **NeoForge Server Log:** [Link here]
*   **Velocity Proxy Log:** [Link here]
*   **Crash Report (if applicable):** [Link here]

---

### Configuration Check
<!-- Please quickly verify your settings to save time. -->

*   [ ] My Velocity `velocity.toml` is set to `player-info-forwarding-mode = "modern"`.
*   [ ] I have checked that the `forwarding.secret` matches on both the proxy and the server.
*   [ ] **Large Modpack?** I have tried adding `-Dvelocity.max-known-packs=128` to my Velocity startup flags.
*   [ ] **Fabric/Sinytra?** I have tried setting `login-custom-packet-catchall = false` in `neovelocity-common.toml`.

### Disconnect Message
<!-- If you were kicked from the server, what was the exact message on the screen? -->

### Additional Context
<!-- Add any other context about the problem here. -->
