# eemove
Automated deployment for ExpressionEngine websites
![alt text][logo]
---

## Build instructions
Includes ANT build file. Compile using `ant compile clean dist`

jar file `eemove.jar` will be created within the newly created **dist** folder.

Move **eemove.jar** to your ExpressionEngine websites root on your local dev and run using `java -jar eemove.jar`, then simply follow the prompts!

## Useage
Push everything to staging environment: `push -d staging all`

Breakdown of the above command:

1. `push` what we're going to do, can be either `push` or `pull`
2. `-d` the "dry run" flag for Rsync. If using `-d` will execute a dry run and no files will actually be copied. To issue a live run use the `-l` flag instead
3. The environment to push/pull to/from
4. What to push/pull. Options are
  1. `all` for everything (doesn't include database; for files only)
  2. `uploads` for the upload directories (both user defined and **images/uploads**)
  3. `templates` for the **system/user/templates** directory
  4. `plugins` for the **system/user/addons** and **app/themes/user** directories
  5. `app` for the public facing directories
  6. `system` for the system directory
  7. `database` for the database (left as separate command; be careful with this one! A backup will be made of both source and destination first should the worst happen). **NOTE** Must use with `-l` flag, will be ignored if using `-d`

## Config file example

```yaml
# Global EE settings

globals:
  ee_system: "system"
  ee_app: "app"
  upload_dir: "uploads" # optional, if using custom upload directory/ies
  above_root: "true" # use true or false to signify whether the system folder is above root or not

# Begin environment specific configuration(s)

local:
  vhost: "http://yoursite.dev"
  ee_path: "/home/john/sites/your_site" # use an absolute path here

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
    password: "password" # Only sent if not using public/private key authentication
    host: "host"
    port: 3306 # Port is optional, will default to 3306, use to overwrite default

  ssh:
    host: "host"
    user: "user"
    password: "password"
    port: 22 # Port is optional, will default to 22, use to overwrite default

# production: # multiple environments can be specified
#  [...]
```

## Additional info
Right now eemove is only configured for ExpressionEngine 3. Once the source is more stable and had more testing will likely add EE2 support.

**NOTE:** eemove assumes your directory structure is with the **system** folder above root.

```
-[ExpressionEngine Local dev folder]
--app
---admin.php
---index.php
---images
----uploads
---themes
---...
--system
---ee
---user
----addons
----templates
----...
--eemove.jar
--eemove.config
--eemove.ignore

```

You can download my EE project bootstrap here: [ee_bootstrap](https://github.com/sparkison/ee_bootstrap) to get started!

### Notice

This project is currently a work in progress... check back soon for updates, fixes and amendments

[logo]: https://github.com/sparkison/eemove/blob/master/eemove.jpg "eemove logo"
