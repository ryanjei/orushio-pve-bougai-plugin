function direction(yaw) {
  const normalized = ((Number(yaw) % 360) + 360) % 360;
  return ['南', '南西', '西', '北西', '北', '北東', '東', '南東'][Math.floor((normalized + 22.5) / 45) % 8];
}

function gaze(pitch) {
  const value = Number(pitch);
  if (value < -60) return '上向き';
  if (value < -20) return 'やや上';
  if (value <= 20) return '水平';
  if (value <= 60) return 'やや下';
  return '下向き';
}

if (typeof module !== 'undefined') module.exports = { direction, gaze };
