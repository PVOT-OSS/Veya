# SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
# SPDX-License-Identifier: AGPL-3.0-or-later
#
# English. Code names meanings; this file holds the words. See Lexicon.kt for the format.

# ---------------------------------------------------------------------------------------------
# Words dropped before anything is matched
# ---------------------------------------------------------------------------------------------

[words utterance.filler]
hey veya | ok veya | veya | please | could you | can you | would you | i want to | i'd like to

# ---------------------------------------------------------------------------------------------
# Numbers
# ---------------------------------------------------------------------------------------------

[values number]
zero = 0
one = 1
two = 2
three = 3
four = 4
five = 5
six = 6
seven = 7
eight = 8
nine = 9
ten = 10
eleven = 11
twelve = 12
thirteen = 13
fourteen = 14
fifteen = 15
sixteen = 16
seventeen = 17
eighteen = 18
nineteen = 19
a = 1
an = 1
half = 0.5
couple = 2
a couple = 2
a couple of = 2

[values number.scale]
hundred = 100
thousand = 1000
million = 1000000
billion = 1000000000

[words number.and]   # "two hundred and five"
and

[values number.tens]
twenty = 20
thirty = 30
forty = 40
fifty = 50
sixty = 60
seventy = 70
eighty = 80
ninety = 90

# ---------------------------------------------------------------------------------------------
# Durations
# ---------------------------------------------------------------------------------------------

[values duration.unit]   # seconds
second = 1
seconds = 1
sec = 1
secs = 1
s = 1
minute = 60
minutes = 60
min = 60
mins = 60
m = 60
hour = 3600
hours = 3600
hr = 3600
hrs = 3600
h = 3600
day = 86400
days = 86400
d = 86400

[words duration.joiner]
and | for

[values duration.fraction]   # a part of the unit that follows
quarter = 0.25
a quarter = 0.25
half = 0.5
a half = 0.5

[words duration.fraction.link]
of | an | a

[words duration.and.half]   # after a unit: "two hours and a half"
a half

# ---------------------------------------------------------------------------------------------
# Times and dates
# ---------------------------------------------------------------------------------------------

[words time.filler]
at | on | for | by | the | this | o'clock | oclock | in the

[words time.relative]   # "in 10 minutes"
in

[words time.next]   # "next friday"
next

[values time.day]   # days from today
today = 0
tonight = 0
tomorrow = 1
day after tomorrow = 2
the day after tomorrow = 2

[values time.meridiem]   # 0 before noon, 1 after
am = 0
a.m. = 0
a.m = 0
pm = 1
p.m. = 1
p.m = 1

[values time.part]   # 0 before noon, 1 after
morning = 0
afternoon = 1
evening = 1
night = 1
tonight = 1

[values time.fixed]   # hour of the day
noon = 12
midday = 12
midnight = 0

[values time.fraction]   # minutes, with time.past or time.to
half = 30
quarter = 15

[words time.past]
past | after

[words time.to]
to | before

[values weekday]   # extra short forms; full names come from the platform
tues = 2
thur = 4
thurs = 4

# ---------------------------------------------------------------------------------------------
# Device
# ---------------------------------------------------------------------------------------------

[intent device.battery]
0.95 ((what's|what is|how much|show|check)( is)? )?((my|the) )?(phone )?battery( (level|percentage|status|life|charge))?( (left|remaining|now))?
0.95 how much (charge|battery)( do i have| is)?( left)?
0.7 .*\bbattery\b.*

[words torch.light]
flashlight | flash light | torch

[intent device.torch.on]
0.95 (turn|switch) on (the )?<torch.light>
0.95 (turn|switch) (the )?<torch.light> on
0.9 <torch.light> on
0.9 (enable|start) (the )?<torch.light>

[intent device.torch.off]
0.95 (turn|switch) off (the )?<torch.light>
0.95 (turn|switch) (the )?<torch.light> off
0.9 <torch.light> off
0.9 (disable|stop) (the )?<torch.light>

[intent device.info]
0.95 (show )?(my )?(device|phone) (info|information|details)
0.95 what (phone|device) is this
0.9 ((what|which) )?android version( is this| am i on| do i have)?

[intent time.now]
0.95 what time is it( now| right now)?
0.95 ((what's|what is|tell me) )?(the )?(current )?time( now| right now)?

[intent date.today]
0.95 ((what's|what is|tell me) )?(the |today's )?date( today)?
0.95 what day is (it|today)( today)?
0.9 what's today

[intent app.open]
0.8 (open|launch|start|run)( the)?( {app})?( app)?

# ---------------------------------------------------------------------------------------------
# Settings pages
# ---------------------------------------------------------------------------------------------

[intent settings.open]
0.95 ((open|show|go to) )?(the )?(system )?settings
0.95 ((open|show|go to) )?(the )?{page=<pages>} settings
0.85 (open|show|go to) (the )?{page=<pages>}( page)?

[words settings.page.wifi]
wifi | wi-fi | wi fi
[words settings.page.bluetooth]
bluetooth
[words settings.page.display]
display | screen | brightness
[words settings.page.sound]
sound | volume
[words settings.page.battery]
battery | battery saver
[words settings.page.location]
location | gps
[words settings.page.notifications]
notification | notifications
[words settings.page.apps]
app | apps | application
[words settings.page.storage]
storage
[words settings.page.security]
security | privacy
[words settings.page.accessibility]
accessibility
[words settings.page.date]
date | time | date and time
[words settings.page.language]
language | languages
[words settings.page.network]
network | internet | mobile data | data usage
[words settings.page.airplane]
airplane | airplane mode | flight mode

# ---------------------------------------------------------------------------------------------
# What connected apps share
# ---------------------------------------------------------------------------------------------

[intent context.search]
0.8 (search|look in|find in) (my )?{app=[\p{L}\p{N} ]+?} for {query=.+}
0.75 (search|find|look up)( for)? {query}( in my apps)?

[words vocabulary.filler]
any | show | show me | find | list | get | give me | search | search for | do i have | do i have any | are there | are there any | what are | what are my | my | all | all my | the | latest | recent

[words vocabulary.link]
from | by | with | about | for | named | called | mentioning | containing | on | of

[words vocabulary.message]   # types that read as messages
messages | message | texts | text

# <types> and <flags> are the words connected apps teach Veya at runtime.
[intent context.vocabulary]
0.95 (?:<vocabulary.filler> )*(?:{flag=<flags>} )?{type=<types>}(?: <vocabulary.link> {subject})?(?: in (?:my )?{app=[\p{L}\p{N} ]+?}(?: app)?)?
0.95 (?:search|look in|find in) (?:my )?{type=<types>} for {subject=.+}

[intent context.vocabulary.messages]
0.95 what did {subject} (?:say|send|text|write)(?: me)?
0.95 (?:<vocabulary.filler> )*{flag=<flags>}

# ---------------------------------------------------------------------------------------------
# Commands handed to other apps. {subject} may end by naming the app: "… in Organic Maps".
# ---------------------------------------------------------------------------------------------

[words command.app.link]
in | on | with | using | via | through

[intent command.app]   # how a subject names an app; tried in this order
1 {rest=.+} <command.app.link> (?:the )?{app=.+?}(?: app)?
1 {rest=.+?} <command.app.link> (?:the )?{app=.+?}(?: app)?
1 <command.app.link> (?:the )?{app=.+?}(?: app)?

[intent command.timer]
0.95 (set|start|create|make|put)( me)?( a| an)? timer   # first: equal confidence keeps the earlier phrase
0.95 (set|start|create|make|put)( me)?( a| an)? timer( for)? {subject}
0.95 (set|start|create|make|put)( me)?( a| an)? {subject} timer
0.9 timer( for)? {subject}
0.9 {subject} timer
0.9 count ?down( from)? {subject}

[intent command.alarm]
0.95 (set|create|make|add|put)( me)?( an| a)? alarm
0.95 (set|create|make|add|put)( me)?( an| a)? alarm( for| at)? {subject}
0.95 wake me( up)?( at| by| for)? {subject}
0.9 alarm( for| at)? {subject}

[intent command.alarms]
0.95 (show|open|list|see|check)( me)?( my| the| all( of)?( my)?)? alarms( {subject})?
0.9 (what|which) alarms( do i have| are set| have i set)( {subject})?

[intent command.map]
0.9 (navigate|directions|get directions|give me directions|show me the way)
0.9 (get |give me |show me |find )?(the )?directions (to|for) {subject}
0.9 navigate( me)? to {subject}
0.9 take me to {subject}
0.9 how (do i|can i|to) get to {subject}
0.9 (show|find|search for|look up)( me)? {subject} on (the |a )?map
0.9 (show|find|search for)( me)? {subject} (near me|nearby)
0.9 where is {subject} on the map

[intent command.web]
0.95 (search the web|search online|web search|search the internet|google something)
0.95 search (the )?(web|internet|online) for {subject}
0.95 (search|look up|search for) {subject} (online|on the web|on the internet)
0.95 web search( for)? {subject}
0.9 google {subject}

[intent command.play]
0.84 play( something)?
0.84 play( me)? {subject}
0.84 listen to {subject}

[intent command.link]
0.95 (open|visit|go to) (a )?(link|website|web address|web page|url|site)
0.95 (open|go to|visit|browse to|load) {subject=(?:https?://)?(?:[\p{L}\p{N}-]+\.)+\p{L}{2,}(?::\d+)?(?:[/?#]\S*)?(?: .+)?}
0.9 {subject=(?:https?://)?(?:[\p{L}\p{N}-]+\.)+\p{L}{2,}(?::\d+)?(?:[/?#]\S*)?}

[intent command.photo]
0.95 (take|snap|capture|shoot)( me)?( a| an| some)? (photo|photos|picture|pictures|pic|pics|selfie|snapshot)( {subject})?

[intent command.video]
0.95 (record|take|shoot|film|capture|make)( me)?( a| some)? (video|videos|clip)( {subject})?

[intent command.audio]
0.95 record( some| an| a| my)? (audio|sound|voice|voice note|voice memo|voice recording|memo)( {subject})?
0.92 start( a| an)?( voice| audio| sound)? recording( {subject})?

[intent command.event]
0.95 (add|create|schedule|make|new)( an| a)? (event|meeting|appointment)( to (my |the )?calendar)?
0.95 (add|put|schedule|create|make)( an| a)?( event| meeting| appointment)?( called| named| for)? {subject} (to|in|on|into) (my |the )?calendar
0.9 (add|schedule|create|make|plan|book)( an| a)? (event|meeting|appointment)( called| named| for)? {subject}
0.85 (add|schedule|plan|book) {subject}

[intent command.call]
0.95 (call|dial)( a number| someone)?
0.95 (call|dial|ring|phone) {subject=\+?[\d(][\d ()-]*\d(?: .+)?}

[intent command.text]
0.95 (text|message|sms)( to)? {number=\+?[\d(][\d ()-]*\d}(?:( saying| that says| that| with)?:? {subject})?
0.95 send (a )?(text|message|sms) to {number=\+?[\d(][\d ()-]*\d}(?:( saying| that says| that| with)?:? {subject})?

# ---------------------------------------------------------------------------------------------
# Built-in answers: arithmetic. Symbols (+ - × ÷ * / ^ % ( )) work in every language.
# ---------------------------------------------------------------------------------------------

[words math.plus]
plus | add
[words math.minus]
minus | less | take away
[words math.times]
times | multiplied by | x
[words math.divide]
divided by | over
[words math.power]
to the power of | to the power | raised to the power of | raised to
[words math.percent]
percent | per cent
[words math.of]
of
[words math.root]
square root of | root of | sqrt
[words math.squared]
squared
[words math.cubed]
cubed
[words math.filler]
the

[intent math.calculate]
0.95 (what's|what is|whats|calculate|compute|how much is|work out|solve|evaluate)( the)? {subject}
0.7 {subject}

# ---------------------------------------------------------------------------------------------
# Built-in answers: unit conversions. Each value is the unit's size in its kind's base unit.
# ---------------------------------------------------------------------------------------------

[values unit.length]   # metres
mm = 0.001
millimeter = 0.001
millimeters = 0.001
millimetre = 0.001
millimetres = 0.001
cm = 0.01
centimeter = 0.01
centimeters = 0.01
centimetre = 0.01
centimetres = 0.01
m = 1
meter = 1
meters = 1
metre = 1
metres = 1
km = 1000
kilometer = 1000
kilometers = 1000
kilometre = 1000
kilometres = 1000
inch = 0.0254
inches = 0.0254
ft = 0.3048
foot = 0.3048
feet = 0.3048
yd = 0.9144
yard = 0.9144
yards = 0.9144
mi = 1609.344
mile = 1609.344
miles = 1609.344
nautical mile = 1852
nautical miles = 1852

[values unit.mass]   # kilograms
mg = 0.000001
milligram = 0.000001
milligrams = 0.000001
g = 0.001
gram = 0.001
grams = 0.001
kg = 1
kilo = 1
kilos = 1
kilogram = 1
kilograms = 1
tonne = 1000
tonnes = 1000
oz = 0.028349523125
ounce = 0.028349523125
ounces = 0.028349523125
lb = 0.45359237
lbs = 0.45359237
pound = 0.45359237
pounds = 0.45359237
stone = 6.35029318
stones = 6.35029318

[values unit.volume]   # litres
ml = 0.001
milliliter = 0.001
milliliters = 0.001
millilitre = 0.001
millilitres = 0.001
cl = 0.01
dl = 0.1
l = 1
liter = 1
liters = 1
litre = 1
litres = 1
tsp = 0.00492892159375
teaspoon = 0.00492892159375
teaspoons = 0.00492892159375
tbsp = 0.01478676478125
tablespoon = 0.01478676478125
tablespoons = 0.01478676478125
fl oz = 0.0295735295625
fluid ounce = 0.0295735295625
fluid ounces = 0.0295735295625
cup = 0.2365882365
cups = 0.2365882365
pint = 0.473176473
pints = 0.473176473
quart = 0.946352946
quarts = 0.946352946
gal = 3.785411784
gallon = 3.785411784
gallons = 3.785411784

[values unit.area]   # square metres
square meter = 1
square meters = 1
square metre = 1
square metres = 1
sq m = 1
square kilometer = 1000000
square kilometers = 1000000
square kilometre = 1000000
square kilometres = 1000000
sq km = 1000000
square foot = 0.09290304
square feet = 0.09290304
sq ft = 0.09290304
square mile = 2589988.110336
square miles = 2589988.110336
acre = 4046.8564224
acres = 4046.8564224
hectare = 10000
hectares = 10000
ha = 10000

[values unit.speed]   # kilometres per hour
km/h = 1
kmh = 1
kph = 1
kilometers per hour = 1
kilometres per hour = 1
mph = 1.609344
miles per hour = 1.609344
m/s = 3.6
meters per second = 3.6
metres per second = 3.6
knot = 1.852
knots = 1.852

[values unit.data]   # bytes
bit = 0.125
bits = 0.125
byte = 1
bytes = 1
kb = 1000
kilobyte = 1000
kilobytes = 1000
mb = 1000000
megabyte = 1000000
megabytes = 1000000
gb = 1000000000
gigabyte = 1000000000
gigabytes = 1000000000
tb = 1000000000000
terabyte = 1000000000000
terabytes = 1000000000000
kib = 1024
mib = 1048576
gib = 1073741824

[values unit.time]   # seconds
second = 1
seconds = 1
sec = 1
secs = 1
minute = 60
minutes = 60
min = 60
mins = 60
hour = 3600
hours = 3600
hr = 3600
hrs = 3600
day = 86400
days = 86400
week = 604800
weeks = 604800
year = 31536000
years = 31536000

[values unit.temperature.zero]   # the unit's reading at 0 °C
c = 0
°c = 0
celsius = 0
degrees celsius = 0
centigrade = 0
f = 32
°f = 32
fahrenheit = 32
degrees fahrenheit = 32
k = 273.15
kelvin = 273.15

[values unit.temperature.degree]   # the unit's degrees per 1 °C
c = 1
°c = 1
celsius = 1
degrees celsius = 1
centigrade = 1
f = 1.8
°f = 1.8
fahrenheit = 1.8
degrees fahrenheit = 1.8
k = 1
kelvin = 1

[words convert.to]
to | in | into | as

[intent math.convert]
0.95 (convert|change) {source} <convert.to> {target}
0.95 how (many|much) {target} (are |is )?(there )?in {source}
0.9 (what's|what is|whats) {source} <convert.to> {target}
0.9 {source} <convert.to> {target}

