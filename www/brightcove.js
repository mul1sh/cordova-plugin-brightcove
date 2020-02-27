var exec = require('cordova/exec');

exports.createPlayer = function (arg0, success, error) {
    exec(success, error, 'BrightcovePlayer', 'createPlayer', [arg0]);
};

exports.pausePlayer = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'pausePlayer', []);
};

exports.resumePlayer = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'resumePlayer', []);
};

exports.suspendPlayer = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'suspendPlayer', []);
};

// gets
exports.getPlayerPlayheadTime = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'getPlayerPlayheadTime', []);
};

exports.getPlayerDuration = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'getPlayerDuration', []);
};

exports.getPlayerState = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'getPlayerState', []);
};

exports.getPlayerBitRate = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'getPlayerBitRate', []);
};



exports.isPlayerInFullScreen = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'isPlayerInFullScreen', []);
};

exports.isPlayerInCastMode = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'isPlayerInCastMode', []);
};

exports.isPlayerPlaying = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'isPlayerPlaying', []);
};

exports.isPlayerClosed = function (success, error) {
    exec(success, error, 'BrightcovePlayer', 'isPlayerClosed', []);
};

//sets
exports.setPlayerPlayheadTime = function (arg0, success, error) {
    exec(success, error, 'BrightcovePlayer', 'setPlayerPlayheadTime', [arg0]);
};

exports.setPlayerSeekable = function (arg0, success, error) {
    exec(success, error, 'BrightcovePlayer', 'setPlayerSeekable', [arg0]);
};

exports.setPlayerFullScreen = function (arg0, success, error) {
    exec(success, error, 'BrightcovePlayer', 'setPlayerFullScreen', [arg0]);
};