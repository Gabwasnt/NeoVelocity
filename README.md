<img src="https://raw.githubusercontent.com/intergrav/devins-badges/d0dd406e1106cd0726bf85743575368e3c4b2f5e/assets/cozy/supported/neoforge_vector.svg" alt="NeoForge Logo" />

# NeoVelocity

**NeoVelocity** is a compatibility mod for NeoForge servers to enable secure and modern player information forwarding
when used in conjunction with a [Velocity proxy](https://papermc.io/software/velocity). By leveraging Velocity’s modern
forwarding protocol, this mod ensures that essential player data—such as IP addresses, UUIDs, and authentication
details—is transmitted reliably from the proxy to your server.

---

## Features

- **Modern Forwarding Support:** Implements Velocity’s native, modern forwarding protocol to securely receive player
  data.
- **Configuration File:** On first startup, the mod creates a default configuration file.

## Installation

1. **Download & Install:**  
   Place the NeoVelocity mod jar file into your NeoForge server’s `mods` folder.

2. **Server Properties:**  
   The `server.properties` file does not require `online-mode=false` since the mod replaces the whole login process.  
   *(If you encounter any issues you can set it to false)*

3. **Restart Your Server:**  
   Launch the server to allow NeoVelocity to initialize. During this process, the mod will automatically generate its
   configuration file at: `config/neovelocity-common.toml`.

4. **Setting The Secret:**  
   Simply open the config file located at: `config/neovelocity-common.toml`, and change the secret to the one assigned
   to your proxy. Usually the secret is inside the file: `forwarding.secret` inside your proxy's running directory. More over, the entry is treated as a path to a file containing the secret, if it ends with `.secret`. This file has to be UTF-8 encoded and non-empty.
   *(This can be done while the server is running the config will be automatically reloaded!)*

5. **Tell Velocity About The Mods:**  
   You need to add `-Dvelocity.max-known-packs=[n]` to launch arguments of Velocity otherwise it won't let you connect.
   Replace `[n]` by the number of mods you have + vanilla default(64) or more if it still gives you errors.
   See [Docs](https://docs.papermc.io/velocity/reference/system-properties#velocitymax-known-packs) for more
   information.

## Usage Notes

- **Port Leaking:**  
  The NeoForge server should not be exposed to the internet, as someone could impersonate the proxy with the correct
  forwarding secret and successfully authenticate any account.

- **Troubleshooting:**  
  **Possible Disconnect Messages**
    - `Unable to verify player details.`:  
      This means your forwarding secret is not the same as the proxy's. Please refer
      to the Installation step 4.
    - `Unsupported forwarding version.`:  
      This means you are not using the right forwarding method on your proxy. If you
      wish for me to implement more methods send me a DM on discord or open an Issue on GitHub.
    - `This server requires you to connect with Velocity.`:  
      This means you are connecting to the server directly, you
      need to be connecting through the proxy.
    - `Unable to connect you to {servername}. Please try again later.`:  
      This is velocity related, make sure Installation
      step 5 is done properly otherwise you might want to see you Velocity logs.

- **Further Documentation:**  
  For comprehensive guidance on setting up and fine-tuning your Velocity proxy—including details on advanced
  configurations and security best practices—please refer to
  the [Velocity Documentation](https://docs.papermc.io/velocity).

## Contacts

You can contact me through my [GitHub](https://github.com/Gabwasnt) or on Discord as `g_ab`

## Feature Request

The project is open source you can contribute anytime. If you encounter any issues or want another compatibility layer
you can contact me.

---

NeoVelocity serves as an essential bridge between NeoForge and Velocity, ensuring that modded servers can benefit from
modern proxy features with minimal setup. Enjoy a more secure, efficient, and modernized server experience!
