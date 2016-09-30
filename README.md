# eemove
Automated deployment for ExpressionEngine websites
![alt text][logo]
---

## Dependencies
eemove utilizes `rsync`, `ssh`, and `sshpass` (if not using keys for authentication). These items will need to be installed on your OS for eemove to work correctly.

You will need to install the Java Runtime Environment (JRE). You find the latest release for you OS here: [Java SE](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
## Included libraries
* [SnakeYAML](https://bitbucket.org/asomov/snakeyaml) - used for digesting the config file
* [Guava](https://github.com/google/guava)

## Build instructions
Includes ANT build file. Compile using `ant compile clean dist`

jar file `eemove.jar` will be created within the newly created **dist** folder.

Or you can download one of the precompiled release via the [releases](https://github.com/sparkison/eemove/releases) tab. (Note: the releases are typically a few commits behind master. For the latest version download and compile the source).

Move **eemove.jar** to wherever you like, then `cd` into your ExpressionEngine websites root on your local dev and run using `java -jar /PATH_TO_EEMOVE/eemove.jar`, and simply follow the prompts!

Alternatively, if you get tired of typing out `java -jar ...` every time, you can add eemove as a bash alias. 
To do so, edit your `bash_profile` (E.g. `nano ~/.bash_profile`) and add the following:
```
# Shortcut for eemove
alias eemove="java -jar /PATH_TO_EEMOVE/eemove.jar"
```

Exit and save your bash profile, and finally reload it (`source ~/.bash_profile`) and you should be able to enter `eemove` at the terminal to display the eemove help screen.

## Useage
Push uploads to staging environment: `eemove push -l staging -u`

Breakdown of the above command:

1. `push` what we're going to do, can be either `push` or `pull`
2. `-l` the "dry run" flag for Rsync. If using `-d` will execute a dry run and no files will actually be copied. To issue a live run use the `-l` flag instead
3. The environment to push/pull to/from
4. What to push/pull. Options are
  1. `all` for everything (doesn't include database; for files only)
  2. `uploads|-u` for the upload directories (both user defined and **images/uploads**)
  3. `templates|-t` for the **system/user/templates** directory
  4. `addons|-a` for the **system/user/addons** and **app/themes/user** directories
  5. `app` for the public facing directories
  6. `system` for the system directory
  7. `custom` specify a custom local and remote directory to push/pull to/from
  8. `update` for the **system/ee** and **app/themes/ee** directories as well as the **system/user/config/config.php** file
  9. `database|-d` for the database (left as separate command; be careful with this one! A backup will be made of both source and destination first should the worst happen). **NOTE** Must use with `-l` flag, will be ignored if using `-d`

Helper commands

1. `eemove` displays an example list of commands
2. `eemove fixperms [environment]` attempts to fix permissions on the environment selected (eg. `eemove fixperms staging`) using the [recommended settings](https://docs.expressionengine.com/latest/installation/installation.html#file-permissions)

## Config file example

```yaml
# Global EE settings

globals:
  ee_version: "3" # ExpressionEngine version, 2 or 3 are currently supported
  ee_system: "system"
  ee_app: "app" # leave blank if system is not above root
  upload_dir: "uploads" # optional, if using custom upload directory/ies
  above_root: "true" # use true or false to signify whether the system folder is above root or not
  
  executables:
    sshpass: "/path/to/sshpass/bin/" # optional, specify path to sshpass, will default to /usr/local/bin/. Type `which sshpass` in terminal to determin where the executable is located
    mysql: "/path/to/mysql/bin/" # optiona, base path to mysql and mysqldump, will default to /usr/local/bin/. Type `which mysql` in terminal to determin where the executable is located

  authentication:
    type: "key" # use either 'key' for public key authentication or 'password' for password
    keyfile: "/Users/JOHN_DOE/.ssh/id_rsa" # optional, only needed if using 'key' for type. Use an absolute path here
    # keypass: "password" # optional, only needed if using 'key' for type and the key file is password protected

# Begin environment specific configuration(s)

local:
  vhost: "http://yoursite.dev"

  database:
    name: "database_name"
    user: "root"
    password: "root"
    host: "127.0.0.1"

staging:
  vhost: "http://example.com"
  ee_path: "/var/www/your_site" # use an absolute path here

  database:
    name: "database_name"
    user: "user"
    password: "password"
    host: "host"
    port: 3306 # Port is optional, will default to 3306, use to overwrite default

  ssh:
    host: "host"
    user: "user"
    password: "password" # Only sent if not using public/private key authentication
    port: 22 # Port is optional, will default to 22, use to overwrite default

# production: # multiple environments can be specified
#  [...]
```

## Additional info

eemove assumes your directory structure is with the **system** folder above root per the [ExpressionEngine best practices](https://docs.expressionengine.com/latest/installation/best_practices.html):

```
[ExpressionEngine Local dev folder]
├── app
|   ├── admin.php
|   ├── index.php
|   ├── images
|   ├── themes
│   └── ....
├── system
|   ├── ee
|   ├── user
│   └── ...
├── eemove.config
└── eemove.ignore
```

You can download my EE project bootstrap here: [ee_bootstrap](https://github.com/sparkison/ee_bootstrap) to get started!

### Notice

This project is currently a work in progress... check back often for updates, fixes and amendments

[logo]: https://github.com/sparkison/eemove/blob/master/resources/images/eemove.jpg "eemove logo"
