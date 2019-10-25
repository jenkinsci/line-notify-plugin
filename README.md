# Jenkins Line Notify Plugin

This plugin use for send message to LINE application, using service [Line notify](https://notify-bot.line.me/en/)

## Prerequisite

- LINE account
- Token from [Line notify](https://notify-bot.line.me/en/)

## Installation

Install plugin "line notify" from `Jenkins > Manage Jenkins > Manage Plugins`

## How to use

### Freestyle job

Just add `Line notify` from build step

### Pipeline job

```shell script
node {
   lineNotify credentialsId: 'my-credential'
}
```
