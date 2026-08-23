const assert = require('node:assert/strict');
const { direction, gaze } = require('../../main/resources/web/ui-format.js');

[
  [0, '南'], [22.49, '南'], [22.5, '南西'], [67.49, '南西'], [67.5, '西'],
  [112.5, '北西'], [157.5, '北'], [202.5, '北東'], [247.5, '東'], [292.5, '南東'],
  [337.49, '南東'], [337.5, '南'], [-22.5, '南'], [-22.51, '南東'], [360, '南']
].forEach(([yaw, expected]) => assert.equal(direction(yaw), expected, `yaw=${yaw}`));

[
  [-90, '上向き'], [-60.01, '上向き'], [-60, 'やや上'], [-20.01, 'やや上'],
  [-20, '水平'], [0, '水平'], [20, '水平'], [20.01, 'やや下'], [60, 'やや下'],
  [60.01, '下向き'], [90, '下向き']
].forEach(([pitch, expected]) => assert.equal(gaze(pitch), expected, `pitch=${pitch}`));

console.log('ui-format boundary tests passed');
