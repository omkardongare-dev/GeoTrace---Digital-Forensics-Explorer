const $ = (id) => document.getElementById(id);
let latestLookup = null;
let latestAnalysis = null;
const map = L.map('map').setView([20, 0], 2);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  maxZoom: 19, attribution: '&copy; OpenStreetMap contributors'
}).addTo(map);
const markers = L.layerGroup().addTo(map);

function safe(value) {
  return String(value ?? 'Not available').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
function setStatus(id, message, isError=false) {
  $(id).textContent = message;
  $(id).style.color = isError ? '#ff9e9e' : '';
}
function showLookup(data) {
  const fields = [
    ['IP address', data.ip], ['City', data.city], ['Region', data.region],
    ['Country', data.country], ['Coordinates', data.latitude != null && data.longitude != null ? `${data.latitude}, ${data.longitude}` : null],
    ['Organization', data.organization], ['ASN', data.asn], ['Time zone', data.timezone]
  ];
  $('lookupResult').innerHTML = fields.map(([k,v]) => `<div class="result-item"><span>${safe(k)}</span><strong>${safe(v)}</strong></div>`).join('');
  latestLookup = data;
  $('focusMap').classList.toggle('d-none', !(Number.isFinite(Number(data.latitude)) && Number.isFinite(Number(data.longitude))));
  if (Number.isFinite(Number(data.latitude)) && Number.isFinite(Number(data.longitude))) addMarker(data);
}
function addMarker(data) {
  const lat = Number(data.latitude), lon = Number(data.longitude);
  if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat)>90 || Math.abs(lon)>180) return;
  const marker = L.marker([lat,lon]).bindPopup(`<b>${safe(data.ip)}</b><br>${safe([data.city,data.region,data.country].filter(Boolean).join(', '))}`);
  marker.addTo(markers);
  map.setView([lat,lon], 5);
  marker.openPopup();
}
$('lookupForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const ip = $('ipInput').value.trim();
  setStatus('lookupStatus', 'Looking up IP metadata…');
  $('lookupResult').innerHTML = '';
  try {
    const response = await fetch(`/api/lookup?ip=${encodeURIComponent(ip)}`);
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Lookup failed.');
    showLookup(data);
    setStatus('lookupStatus', `Lookup completed. Source: ${data.source}. ${data.note}`);
  } catch (err) {
    setStatus('lookupStatus', err.message || 'Lookup failed.', true);
  }
});
$('focusMap').addEventListener('click', () => {
  if (latestLookup) addMarker(latestLookup);
});

$('uploadForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const file = $('csvFile').files[0];
  if (!file) return;
  const body = new FormData();
  body.append('file', file);
  setStatus('analysisStatus', 'Reading file, calculating SHA-256, and analyzing rows…');
  try {
    const response = await fetch('/api/analyze', {method:'POST', body});
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Analysis failed.');
    latestAnalysis = data;
    $('metricEvents').textContent = data.totalValidEvents;
    $('metricIps').textContent = data.uniqueIpCount;
    $('metricInvalid').textContent = data.invalidRows;
    $('metricHash').textContent = data.sha256.slice(0,12) + '…';
    $('metricHash').title = data.sha256;
    $('analysisSummary').innerHTML = `<div class="notice"><div><b>File:</b> ${safe(data.fileName)} · <b>Size:</b> ${data.fileSizeBytes} bytes · <b>Processed:</b> ${safe(data.processedAt)}</div><div class="mt-2"><b>SHA-256:</b><div class="hash">${safe(data.sha256)}</div></div><div class="mt-2">${safe(data.note)}</div></div>`;
    $('topIps').innerHTML = data.topIps.length ? data.topIps.slice(0,20).map(item => `<tr><td>${safe(item.ip)}</td><td>${item.count}</td><td><button class="btn btn-sm btn-outline-light lookup-from-log" data-ip="${safe(item.ip)}">Lookup</button></td></tr>`).join('') : '<tr><td colspan="3">No valid IP addresses found.</td></tr>';
    $('events').innerHTML = data.events.slice(0,200).map(e => `<tr><td>${safe(e.timestamp)}</td><td>${safe(e.ip)}</td><td>${safe(e.event)}</td></tr>`).join('') || '<tr><td colspan="3">No valid events.</td></tr>';
    $('exportResults').classList.remove('d-none');
    setStatus('analysisStatus', `Analysis complete: ${data.totalValidEvents} valid events, ${data.uniqueIpCount} unique IPs, ${data.invalidRows} invalid rows.`);
    document.querySelectorAll('.lookup-from-log').forEach(button => button.addEventListener('click', () => {
      $('ipInput').value = button.dataset.ip;
      $('lookupForm').requestSubmit();
      window.scrollTo({top:0,behavior:'smooth'});
    }));
  } catch (err) {
    setStatus('analysisStatus', err.message || 'Analysis failed.', true);
  }
});
$('exportResults').addEventListener('click', () => {
  if (!latestAnalysis) return;
  const quote = v => `"${String(v ?? '').replace(/"/g,'""')}"`;
  const lines = [['timestamp','ip','event','source_row'], ...latestAnalysis.events.map(e => [e.timestamp,e.ip,e.event,e.row])];
  const csv = lines.map(row => row.map(quote).join(',')).join('\r\n');
  const url = URL.createObjectURL(new Blob([csv], {type:'text/csv;charset=utf-8;'}));
  const a = document.createElement('a');
  a.href = url; a.download = 'geotrace-analysis.csv'; a.click();
  URL.revokeObjectURL(url);
});
