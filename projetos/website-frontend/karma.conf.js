module.exports = function (config) {
    config.set({
        browsers: ['Chrome', 'ChromeHeadlessCustom'],
        customLaunchers: {
            ChromeHeadlessCustom: {
                base: 'ChromeHeadless',
                flags: ['--no-sandbox', '--disable-setuid-sandbox']
            }
        },
        singleRun: true
    });
};