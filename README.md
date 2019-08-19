[version]: https://img.shields.io/github/tag/pablo67340/getback.svg
[download]: https://img.shields.io/github/release/pablo67340/getback.svg
[discord-invite]: https://discord.gg/v7D6pCm
[license]: https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg
[FAQ]: https://img.shields.io/badge/Wiki-FAQ-blue.svg
[Troubleshooting]: https://img.shields.io/badge/Wiki-Troubleshooting-red.svg
[ ![version][] ][download]
[ ![license][] ](https://github.com/pablo67340/GetBack/tree/master/LICENSE)
[ ![Discord](https://discordapp.com/api/guilds/464333963633098772/widget.png) ][discord-invite]
[ ![FAQ] ](https://github.com/pablo67340/GetBack/wiki/FAQ)
[ ![Troubleshooting] ](https://github.com/pablo67340/GetBack/Troubleshooting)

<img align="right" src="https://i.imgur.com/GCZVCyQ.png" height="200" width="200">

# GetBack (Java GET/Backend)
GetBack is an attempt to create a framework Java developers can use to put their skills to the test and make websites! Typically websites
involve installing a stack, LAMP, WAMP, JAMP, XAMPP, ETC. GetBack aims to eliminate
the use for almost all of those. GetBack is a lightweight HTTP server that utilizes a WebsitePlugin framework to provide you
an easy to use server that can comminucate using standard MySQL or other data methods. GetBack supports javascript powered
websites such as Angular or React. GetBack only requires Java, MySQL to be installed(only if you plan on using MySQL).

## GetBack 1.x
GetBack is currently in early development and much, if not all of the code is subject to change. Features will be added and bugs squashed,
with your cooperation this process will be speedy. Currently 1.x is considered to be unstable, as most of this code is proof of concept.
GetBack should become stable as we reach 2.X. Please keep up to date with the wiki pages as the API itself will change too.

1. [Websites & Plugins](#websiteplugins--websites)
2. [WebsitePlugin Example](#websiteplugin-example)
3. [Backend Example](#backend-example)
4. [More Examples](#more-examples)
5. [Configuration](#configuration)
6. [Extra Classes & Tools worth noticing](#extra-classes--tools-worth-noticing)
7. [Download](#download)
8. [Documentation](#documentation)
9. [Getting Help](#getting-help)
9. [dependencies](#dependencies)
10. [Related Projects](#related-projects)

## WebsitePlugins & Websites

A WebsitePlugin is a Java jar file containing the files needed to host the front and backend of a website. It will contain all front end files & backend functions.
These JAR's can be dropped into the plugins folder to be loaded during next startup. Each WebsitePlugin must contain a plugin.yml (containing plugin specific details), 
and a config.yml (containing webserver specific details). Currently, the website's assets are located in the root directory of the plugin. This means you can either 
put your web files inside the `src` folder while working on it (but it may clutter your IDE), or you can bundle the files into your jar after you export/build it. 
I would recommend using ForStore, its a convenient bundler I designed to make bunding depencies easier. You can package the website files using ForStore using the following steps:

1. Download [ForStore](https://github.com/pablo67340/getback/releases)
2. Create a folder called `ForStore`, move `ForStore.jar` inside.
3. Create a folder called `tmp`
4. Copy all your website's assets into the tmp folder. This means when you navigate into `tmp`, you should see the root of your website.
5. Open cmd/terminal, type `cd path/to/ForStore/ForStore.jar` (replace path/to/ with your actual path off your PC)
6. Execute `java -jar ForStore.jar target.jar` (replace target.jar with your plugin's name)
7. Complete! Your web assets are now bundled inside the jar and ready for server usage! 

[Read More](https://github.com/pablo67340/forstore)

## WebsitePlugin Example

Creating a WebsitePlugin is not as difficult as you think. Once you extend WebsitePlugin into your class, you will be granted access
to all GetBack has to offer for your plugin. After a few required lines of code, you will be hosting your website in no time! 

**Example**:

```java
public class Main extends WebsitePlugin{
	
	private SQLSaver saver;
	
	private static Main INSTANCE;
	
	@Override
	public void onEnable() {
		INSTANCE = this;
		// Save the default config, anchor from child
		saveDefaultConfig(this.getClass());
		String sqlHost = getConfig().getString("sqlHost");
		String sqlUser = getConfig().getString("sqlUser");
		String sqlPassword = getConfig().getString("sqlPassword");
		String sqlDatabase = getConfig().getString("sqlDatabase");
		int sqlPort = getConfig().getInt("sqlPort");
		
		saver = new SQLSaver(sqlHost, sqlDatabase, sqlUser, sqlPassword, sqlPort);
		
		this.getAPILoader().registerAPIClass(user.class);
		
	}
	
	public SQLSaver getSQLSaver() {
		return saver;
	}
	
	public static Main getInstance() {
		return INSTANCE;
	}

}

```

**Note**: By default GetBack will not make any SQL connections for you. This is entirely up to you.
To assist those who would like to use SQL, there is a class to make your life much easier called
the SQLSaver. This class will give you all the basic methods you need to create a functioning
website backend using SQL. If you'd like to integrate SQLite, I'd suggest checking out SQLiteLib!

#### Backend Example:

**Basic User Backend Class (Register, Login, Logout)**:

```java
public class user {

	public static String register(String[] args) {
		try {
			Map<String, Object> params = paramify(args);
			String username = String.valueOf(params.get("username"));
			String password = String.valueOf(params.get("password"));
			String email = String.valueOf(params.get("email"));
			PasswordAuthentication auth = new PasswordAuthentication();
			long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
			password = auth.hash(password);

			if (username.matches("/[\\'^£$%&*()}{@#~?><>,|=_+¬-]/")) {
				return JSONObjects.getStatusFailure("Username can only contain a-z 0-9");
			}

			if (username.contains(" ")) {
				return JSONObjects.getStatusFailure("Username cannot contain spaces");
			}

			ResultSet result = Main.getInstance().getSQLSaver().getConnection().createStatement()
					.executeQuery("SELECT email FROM users WHERE email = '" + params.get("email") + "'");
			if (result.first()) {
				return JSONObjects.getStatusFailure("An account with that E-Mail already exists");
			}

			result = Main.getInstance().getSQLSaver().getConnection().createStatement()
					.executeQuery("SELECT username FROM users WHERE username = '" + params.get("username") + "'");
			if (result.first()) {
				return JSONObjects.getStatusFailure("An account with that username already exists");
			}
			UUID uid = UUID.randomUUID();
			Integer rst = Main.getInstance().getSQLSaver().getConnection().createStatement()
					.executeUpdate("INSERT INTO users (username, token, password, avatar, email, timestamp) VALUES ('"
							+ username + "', '" + uid.toString() + "', '" + password + "', 'NULL', '" + email + "', '"
							+ seconds + "')");
			if (rst == 1) {
				JSONObject data = new JSONObject();
				data.put("tfa", false);
				data.put("token", uid.toString());
				data.put("username", username);
				return JSONObjects.getStatusOk(data.toJSONString());
			} else {
				return JSONObjects.getStatusFailure("SQL returned satus 0");
			}

		} catch (NumberFormatException | SQLException e) {
			System.out.println("Error in get: " + e.toString());
			return JSONObjects.getStatusError("Error in get: " + e.toString());
		}
	}

	public static String login(String[] args) {
		try {
			Map<String, Object> params = paramify(args);
			String username = String.valueOf(params.get("username"));
			String password = String.valueOf(params.get("password"));
			PasswordAuthentication auth = new PasswordAuthentication();

			ResultSet result = Main.getInstance().getSQLSaver().getConnection().createStatement().executeQuery(
					"SELECT id,username,password,email FROM users WHERE username = '" + params.get("username") + "';");
			if (!result.first()) {
				System.out.println("Null");
			}
			String password2 = result.getString("password");

			if (auth.authenticate(password, password2)) {
				// Logged in
				UUID uid = UUID.randomUUID();

				Integer rst = Main.getInstance().getSQLSaver().getConnection().createStatement().executeUpdate(
						"UPDATE users SET token = '" + uid.toString() + "' WHERE username = '" + username + "';");

				if (rst == 1) {
					JSONObject data = new JSONObject();
					data.put("tfa", false);
					data.put("token", uid.toString());
					data.put("username", username);
					System.out.println("JSON:" + JSONObjects.getStatusOk(data));
					return JSONObjects.getStatusOk(data);
				} else {
					return JSONObjects.getStatusFailure("SQL returned satus 0");
				}

			} else {
				// Incorrect password
				return JSONObjects.getStatusFailure("Incorrect Password");
			}
		} catch (NumberFormatException | SQLException e) {
			System.out.println("Error in login: " + e.toString());
			return JSONObjects.getStatusError("Error in login: " + e.toString());
		}
	}

	public static String verifylogin(String[] args) {
		try {
			Map<String, Object> params = paramify(args);
			String token = String.valueOf(params.get("_t"));
			System.out.println("Token: " + token);

			ResultSet result = Main.getInstance().getSQLSaver().getConnection().createStatement().executeQuery(
					"SELECT username,token,avatar,timestamp,id FROM users WHERE token = '" + token + "';");
			if (result.first()) {
				System.out.println("Logged in:");
				JSONObject obj = new JSONObject();
				obj.put("tfa", false);
				obj.put("token", token);
				obj.put("username", result.getString("username"));
				obj.put("id", String.valueOf(result.getInt("id")));
				// TODO: Return banned, avatar, isAdmin, etc.
				return JSONObjects.getStatusOk(obj);
			} else {
				System.out.println("No token found");
				return JSONObjects.getStatusFailure();

			}

		} catch (NumberFormatException | SQLException e) {
			System.out.println("Error in login: " + e.toString());
			e.printStackTrace();
			return JSONObjects.getStatusError("Error in login: " + e.toString());
		}
	}

	public static String logout(String[] args) {
		try {
			Map<String, Object> params = paramify(args);
			Integer userid = (Integer) params.get("userid");

			Integer rst = Main.getInstance().getSQLSaver().getConnection().createStatement()
					.executeUpdate("UPDATE users SET token = NULL WHERE id = '" + userid + "';");
			if (rst == 1) {
				return JSONObjects.getStatusOk();
			} else {
				return JSONObjects.getStatusFailure("SQL returned status 0.");
			}
		} catch (NumberFormatException | SQLException e) {
			System.out.println("Error in logout: " + e.toString());
			e.printStackTrace();
			return JSONObjects.getStatusError("Error in login: " + e.toString());
		}
	}


	// Converts params sent in as String E.g (users=10) to the proper Data Dype
	// variable
	// in the format of Map<String, Object>
	public static Map<String, Object> paramify(String[] args) throws NumberFormatException {
		Map<String, Object> localParams = new HashMap<>();
		for (String arg : args) {
			String param = StringUtils.substringBefore(arg, "=");
			if (param.equalsIgnoreCase("userid")) {
				System.out.println("UserID");
				Integer value = Integer.parseInt(StringUtils.substringAfter(arg, "="));
				localParams.put(param, value);
			} else if (param.equalsIgnoreCase("scriptid")) {
				Integer value = Integer.parseInt(StringUtils.substringAfter(arg, "="));
				localParams.put(param, value);
			} else {
				String value = StringUtils.substringAfter(arg, "=");
				value = value.replace("%20", " ");
				localParams.put(param, value);
			}
		}
		return localParams;
	}

}
```

### More Examples
We provide Examples in the [Example Directory](https://github.com/pablo67340/GetBack/tree/master/src/examples/java).

In addition you can look at the many Website Plugins that were implemented using GetBack:
- [NorthSell](https://github.com/pablo67340/NorthSell)
- (Send us your repo link to have it featured!)

[And many more!](https://github.com/search?q=GetBack+website&type=Repositories&utf8=%E2%9C%93)

> **Note**: In these examples we override methods from the inheriting class `WebsitePlugin`.<br>
> The usage of the `@Override` annotation is recommended to validate methods.
> Each method must be `public` and `static`! Your API backend classes will not be loaded into objects and ran, but are executed using Reflection.

## Configuration

When GetBack loads a plugin, it initially looks for 2 files. plugin.yml, config.yml. You must create these inside your plugin!
Also ensure you are running `saveDefaultConfig(this.getClass());` inside your plugin's `onEnable` method. GetBack
uses the plugin.yml to understand which file inside the plugin needs to be loaded first, followed by a few other
settings. The config.yml will be used to configure the Webserver information, SQL credentials, and other
essential settings.

In terms of development, the Configuration API is actually based off you're beloved, and well known, [Spigot API](https://github.com/SpigotMC/Spigot-API)! This means
accessing your configs are as easy as `getConfig().getString("path.to.node");`

Basic Plugin YML:

```yaml
name: NorthSell
main: website.bryces.northsell.main.Main
indexFile: 'index.html'
forceIndex: true
```

[More Details](https://github.com/pablo67340/GetBack/wiki/Plugin-YML)

Basic Config YML:

```yaml
host: '127.0.0.1'
port: 81

sqlHost: '127.0.0.1'
sqlPort: 3306
sqlDatabase: 'shop'
sqlUser: 'root'
sqlPassword: ''
```
[More Details](https://github.com/pablo67340/GetBack/wiki/Config-YML)


### Extra Classes & Tools worth noticing:

- [PasswordAuthentication](https://github.com/pablo67340/GetBack/blob/master/src/org/getback4j/getback/runnable/PasswordAuthentication.java)
- [Identicon](https://github.com/pablo67340/GetBack/blob/master/src/org/getback4j/getback/runnable/Identicon.java)
- [JSONObjects](https://github.com/pablo67340/GetBack/blob/master/src/org/getback4j/getback/json/JSONObjects.java)
- [SQLSaver](https://github.com/pablo67340/GetBack/blob/master/src/org/getback4j/getback/data/SQLSaver.java)

## Download
Latest Stable Version: [GitHub Release](https://github.com/pablo67340/getback/releases/latest)
Latest Version:
[ ![version][] ][download]

Be sure to replace the **VERSION** key below with the one of the versions shown above!

**Maven**
```xml
<dependency>
    <groupId>website.bryces</groupId>
    <artifactId>GetBack</artifactId>
    <version>VERSION</version>
</dependency>
```
```xml
<repository>
    <id>jcenter</id>
    <name>jcenter-bintray</name>
    <url>http://jcenter.bintray.com</url>
</repository>

```

The builds are distributed using JCenter through Bintray [JDA JCenter Bintray](https://bintray.com/pablo67340/maven/GetBack/)


## Documentation
[Wiki Section](https://github.com/pablo67340/GetBack/wiki)


## Console Commands
There are various default console commands included with GetBack:

1. help: lists all console commands and descriptions.
2. disable {id/name}: disables the specified website plugin.
3. enable {id/name}: enables the specified website plugin.
4. quit: closes GetBack.
5. restart: Restarts the GetBack instance.

Custom commands can be added to GetBack by extending from the Command class, and running addCommand() in your GetBack instance.
You can find more details in the [Wiki Section](https://github.com/pablo67340/GetBack/wiki).

## Getting Help

For general troubleshooting you can visit our wiki [Troubleshooting](https://github.com/pablo67340/GetBack/wiki/Troubleshooting)-Troubleshooting) and [FAQ](https://github.com/pablo67340/GetBack/wiki/FAQ)-FAQ).
<br>If you need any support, or you would just like to chat with me, or other Devs, you can join the [Abstract Studios Discord Guild][discord-invite].

Once you joined, you can find GetBack help in the `#getback` channel.

For guides and setup help you can also take a look at the 

1. [Wiki](https://github.com/pablo67340/GetBack/wiki)
2. [Getting Started](https://github.com/pablo67340/GetBack/wiki/Getting-Started)
3. [Setup](https://github.com/pablo67340/GetBack/wiki/Setup)

## Dependencies:
This project requires **Java 8**.<br>
 * Apache Commons Lang
   * Version: **2.6**
   * [Github](https://github.com/apache/commons-lang)
   * [JCenter Repository](https://bintray.com/bintray/jcenter/commons-lang%3Acommons-lang)
 * JSONS Simple
   * Version: **1.1**
   * [Github](https://github.com/fangyidong/json-simple)
   * [JCenter Repository](https://bintray.com/bintray/jcenter/com.googlecode.json-simple%3Ajson-simple#)
 * MySQL Connector/J
   * Version: **8.0.11**
   * [GitHub](https://github.com/mysql/mysql-connector-j)
   * [JCenter Repository](https://bintray.com/bintray/jcenter/mysql:mysql-connector-java)
 * SnakeYAML
   * Version: **1.23**
   * [Github](https://github.com/asomov/snakeyaml)
   * [JCenter Repository](https://bintray.com/bintray/jcenter/org.yaml%3Asnakeyaml)
 * Guava
   * Version: **27.0.1**
   * [GitHub](https://github.com/google/guava)
   * [JCenter Repository](https://bintray.com/bintray/jcenter/com.google.guava:guava)
 * Lombok
   * Version: **1.18.4**
   * [GitHub](https://github.com/rzwitserloot/lombok)
   * [JCenter Repository](https://bintray.com/bintray/jcenter/org.projectlombok:lombok)

## Related Projects

- [ForStore](https://github.com/pablo67340/ForStore)
- [NorthSell](https://github.com/pablo67340/NorthSell)
- [SQLiteLib](https://github.com/pablo67340/SQLiteLib)
