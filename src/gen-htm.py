import base64
import json
import re

fp = open('messages.json', 'r')
j = json.load(fp)

threads = {}

def msg(a):
    # type 1 - incoming
    # type 2 - outgoing
    fields = ['_id', 'address', 'body', 'date', 'type'] #, 'cl', 'base64']
    out = {}
    for f in fields:
        if f in a:
            out[f] = a[f]
    return out

def phone(a):
    return re.sub('[^0-9]', '', a)[-10:]


for m in j['sms']:
    thread_id = m['thread_id']
    if not thread_id in threads:
        threads[thread_id] = []
    threads[thread_id].append( msg(m) )

for m in j['mms']:
    thread_id = m['thread_id']
    if not thread_id in threads:
        threads[thread_id] = []

    mid = m['_id']

    parts = []
    for part in m['parts']:
        if part['ct'] == 'application/smil':
            continue

        if part['ct'] == 'text/plain':
            parts.append(part)
            continue

        if not 'base64' in part or not 'cl' in part:
            continue

        pid = part['_id']

        # save image
        #part['base64'] = 'trunc'
        #print(part)
        #continue
        ext = ''
        if part['ct'] == 'image/jpeg':
            ext = '.jpg'
        elif part['ct'] == 'image/png':
            ext = '.png'
        elif part['ct'] == 'video/3gpp':
            ext = '.3gpp'
        elif part['ct'] == 'video/mp4':
            ext = '.mp4'

        filename = 'images/' + str(mid) + '-' + str(pid) + ext
        fp = open(filename, 'wb')
        fp.write( base64.b64decode(part['base64']) )
        fp.close()

        part['base64']=''
        part['__filename'] = filename
        parts.append(part)

    m['parts'] = parts
    addresses = []
    for address in m['addresses']:
        if address['type'] == 151:
            addresses.append( phone(address['address']) )
    m['_addresses'] = addresses

    threads[thread_id].append( m )

def id(m):
    return m['_id']

def byDate(a, b):
    if a['_id'] < b['_id']:
        return -1
    elif a['_id'] > b['_id']:
        return 1
    else:
        return 0

fp = open('index.html', 'w+')
fp.write("""
<!DOCTYPE html>
<html lang="en">
<head>
<style>
.me {
    margin-left: 100px;
}
</style>
</head>
<body>
""")


for thread_id in threads:
    threads[thread_id].sort(key=id)

    out = "<h1>" + str(thread_id) + "</h1>"

    for m in threads[thread_id]:
        if 'parts' in m:
            cl = ''
            if m['m_type'] == 132:
                cl=''
            else:
                cl="me"
            for part in m['parts']:
                #if m['type'] == 1:
                # TODO: fix this mess

                if part['ct'] == 'text/plain':
                    out += '<div class="' + cl + '">' + part['body'] + '</div>'
                elif part['ct'] == 'image/jpeg':
                    out += '<div class="' + cl + '"><img src="' + part['__filename'] + '" /></div>'
                elif part['ct'] == 'video/3gpp':
                    out += '<div class="' + cl + '">' + part['__filename'] + '</div>'
                elif part['ct'] == 'video/mp4':
                    out += '<div class="' + cl + '">' + part['__filename'] + '</div>'
        else:
            if m['type'] == 1:
                out += '<div class="">from: ' + phone(m['address']) + "<br />" + m['body'] + '</div>'
            else:
                out += '<div class="me">' + m['body'] + '</div>'
    fp.write(out)

fp.close()

