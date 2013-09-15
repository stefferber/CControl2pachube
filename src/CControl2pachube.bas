
'IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII
'IIIIIIIIIIIIIII       CControl2pachube           IIIIIIIIIIIIIIIIIIIII
'IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII
'  Demo for Temperature, Barmeter, Hygrometer C-Control I 1.1
'  Connect over serial port with a PC to CControl2pachube.java code
' $Id: CControl2pachube.bas,v 1.2 2009/02/05 23:01:37 Fb Exp $
' $Log: CControl2pachube.bas,v $
' Revision 1.2  2009/02/05 23:01:37  Fb
' Added Timestamp and format for CControl2pachube.java
' based on WETTERSTATION TBH 1
'

'--------------------------
'------ I/O PORTS ---------
'--------------------------
DEFINE TEMP_IN        ad[1] ' Temperature Port 1
DEFINE BARO_IN        ad[2] ' Barometer   Port 2
DEFINE HYG_IN         ad[3] ' Hygrometer  Port 3

'--------------------------
'----- SENSOR MEMORY ------
'--------------------------
DEFINE update        BIT[1]
DEFINE TRUE 1
DEFINE FALSE 0
DEFINE temp          WORD[2]
DEFINE baro          WORD[3]
DEFINE hyg           WORD[4]
DEFINE temp_last     WORD[5]
DEFINE baro_last     WORD[6]
DEFINE hyg_last      WORD[7]

'--------------------------------------------------
' --------------- INITIALIZATION    ---------------
'--------------------------------------------------
#init
temp_last=0:baro_last=0:hyg_last=0
update=TRUE
PRINT "$Id: CControl2pachube.bas,v 1.2 2009/02/05 23:01:37 Fb Exp $"
PRINT "date-time-stamp temp hyg baro"


'--------------------------------------------------
' --------------------- LOOP ----------------------
'--------------------------------------------------
#loop
#temperatur
temp = 10*(TEMP_IN-50)/2
IF temp <> temp_last THEN update=TRUE
temp_last = temp

#hygromether
looktab hygtab,HYG_IN,hyg
IF hyg <> hyg_last THEN update=TRUE
hyg_last = hyg

#barometer
looktab barotab,BARO_IN,baro
IF baro <> baro_last THEN update=TRUE
baro_last = baro

IF update=FALSE THEN GOTO wait

IF DAY < 10 THEN PRINT "0";
PRINT DAY;".";
IF MONTH < 10 THEN PRINT "0";
PRINT MONTH;".";
IF YEAR < 10 THEN PRINT "0";
PRINT YEAR;" ";
IF HOUR < 10 THEN PRINT "0";
PRINT HOUR;":";
IF MINUTE < 10 THEN PRINT "0";
PRINT MINUTE;":";
IF SECOND < 10 THEN PRINT "0";
PRINT SECOND;" ";
PRINT temp/10; "."; ABS(temp MOD 10); " ";
PRINT hyg; " ";
PRINT baro; " "

'--------------------------------------------------
' --------------------- WAIT ----------------------
'--------------------------------------------------
#wait
PAUSE 250
GOTO loop


'--------------------------
'----- Lookup Tables ------
'--------------------------
table hygtab "hyg.tab"
table barotab "baro.tab"
