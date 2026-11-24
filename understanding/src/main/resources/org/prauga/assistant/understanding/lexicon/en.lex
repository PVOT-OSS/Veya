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
