FROM 192.168.6.110:5000/alpine_with_encoding:3.9

LABEL maintainer="seonho_choi@tmax.co.kr"

ADD ./hypercloud4-webhook /home/tmax/hypercloud4-webhook
ADD ./start.sh /home/tmax/
RUN chmod 775 -R /home/tmax/hypercloud4-webhook ; \
	chmod 775 /home/tmax/start.sh ; \
	sed -i 's/\r$//' /home/tmax/start.sh ; \
	apk update ; \
	apk upgrade ; \
	apk add tzdata

CMD ["/bin/sh", "-c", "/home/tmax/start.sh"]
