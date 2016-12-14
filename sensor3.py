import time
import RPi.GPIO as GPIO
from bluetooth import *

global i
GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)
GPIO.setup(14,GPIO.IN)   #Front Echo
GPIO.setup(15,GPIO.OUT)  #Front Trig
GPIO.setup(23,GPIO.IN)   #Left Echo
GPIO.setup(24,GPIO.OUT)  #Left Trig
GPIO.setup(17,GPIO.IN)   #Right Echo
GPIO.setup(27,GPIO.OUT)  #Right Trig
GPIO.setup(19,GPIO.OUT)  #warning signal
pwm=GPIO.PWM(19,100)

server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

advertise_service( server_sock, "SampleServer",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ],
                    )
print "Waiting for connection on RFCOMM channel %d" % port

def count(int):
    time_us=int/80
    distance=time_us*5
    print("distance="+str(distance)+"cm")
    return distance

def getdistance():
    global i
    GPIO.output(15,True)
    time.sleep(0.0001)
    GPIO.output(15,False)
    i=0
    while(GPIO.input(14)!=1):
        i=0
    while(GPIO.input(14)==1):
        i=i+1
    distance=count(i)
    return distance

def getdistance_2(b,c):
    GPIO.output(b,True)
    time.sleep(0.0001)
    GPIO.output(b,False)
    a=0
    while(GPIO.input(c)!=1):
        a=0
    while(GPIO.input(c)==1):
        a=a+1
    distance=count(a)
    return distance


connected=0
client_sock,client_addr=server_sock.accept()
print "Accepted connection from ", client_addr
connected=1
obs=0
people=0
pwm.start(50)
try:
    data = client_sock.recv(1024)
    if data=="start":
        print "received [%s]" % data
        client_sock.send("1!")
        client_sock.close()
        connected=0
except IOError:
    pass

while(connected!=1):
    dis=getdistance()
    dis_left=getdistance_2(24,23)
    dis_right=getdistance_2(27,17)
    pwm.ChangeDutyCycle(0)
    if dis<20:
        dis=500
    if dis_left<20:
        dis_left=500
    if dis_right<20:
        dis_right=500
    if 20<dis<300 or 20<dis_left<100 or 20<dis_right<100:
        pwm.ChangeDutyCycle(50)
        duty=min(dis,dis_left,dis_right)
        pwm.ChangeFrequency(950-duty*3)
    else:
        pwm.ChangeDutyCycle(0)
    if dis<300:
        pwm.ChangeFrequency(950-dis*3)
        pwm.ChangeDutyCycle(50)
        time.sleep(1)
        dis2=getdistance()
        if dis2<300:
            pwm.ChangeFrequency(950-dis2*3)
        else:
            pwm.ChangeDutyCycle(0)
        if(0<dis-dis2<300):
            obs=obs+1
            print("static")
        else:
            people=people+1
            print("moving")
    server_sock.settimeout(2)
    try:
        client_s,client_addr=server_sock.accept()
        print "Accepted connection from ", client_addr
        connected=1
    except BluetoothError:
       connected=0
        pass
if(connected==1):
    try:
        data = client_s.recv(1024)
        if data=="finish":
            print "received [%s]" % data
            #client_s.send(str(people)+":"+str(obs)+"!")
            client_s.send(str(obs)+"!")
            print "sent"
            time.sleep(1)
    except IOError:
        pass
        client_sock.close()
        server_sock.close()