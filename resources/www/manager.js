'use strict';

const electron = require('electron');
const http = require('http');
const exec = require("child_process").exec;

const app = electron.app;
const BrowserWindow = electron.BrowserWindow;

let managerWindow = null;
let proc = null;
let interval = null;

app.on('before-quit', () => {
    if (proc) {
        process.exit(0);
    }
});

const startCamelot = () => {
    proc = exec("./camelot-desktop.sh");
};

app.on('ready', () => {
    managerWindow = new BrowserWindow({
        width: 800,
        height: 600,
        frame: false
    });

    managerWindow.loadURL('file://' + __dirname + '/manager.html');
    managerWindow.webContents.openDevTools();

    managerWindow.on('closed', () => {
        if (process.platform !== 'darwin') {
            managerWindow = null;
            app.quit();
        }
    });

    startCamelot();
});
