****************************************************************************
*  Filename    : ASM120THost.sml
*  Description : Pfile for the HOST side of the asm
*  Author      : tzinfo
*  Date        : 10/31/2016
*
****************************************************************************
* Find out MAX_SVIDREQ, MAX_IMAGES & MAX_COMPARM values
#define MAX_STRIP_MAP_DATA_LENGTH   12000
#define MAX_VARIDS      50
#define MAX_COMPARM     30

****************************************************************************
* S?F0    Abort Transaction                                    E <-> H     *
****************************************************************************

S1F0 = s1f0out OUTPUT.

S1F0 = s1f0in INPUT.

S9F0 =s9f0out OUTPUT.

S9F0 =s9f0in INPUT.

****************************************************************************
* S1F1    Are You There/Online? (R)                            H <-> E     *
****************************************************************************

S1F1 = s1f1in INPUT W.

S1F1 = s1f1out OUTPUT W.

****************************************************************************
* S1F2    On Line Data (D)                                     E <-> H     *
****************************************************************************

S1F2 =s1f2out OUTPUT
  <L [0]>.
S1F2 =s1f2in INPUT
  <L [2]
      <A [MAX 6] >               = Mdln
      <A [MAX 6] >               = SoftRev
  >.
****************************************************************************
* S1F3    Select Equipment Status Request                      E <- H     *
****************************************************************************
S1F3 =s1f3out OUTPUT W
  <L [0]>.

S1F3 =s1f3singleout OUTPUT W
<L [1]
    <U2 [1]> = SVID
>.

S1F3 = s1f3statecheck OUTPUT W
 <L [3]
    <U2 [1]> = EquipStatus
    <U2 [1]> = PPExecName
    <U2 [1]> = ControlState
>.

S1F3 = s1f3rcpandstate OUTPUT W
 <L [3]
    <U2 [1]>=EquipStatus
    <U2 [1]>=PreProcessStatus
    <U2 [1]>=PPExecName
>.

S1F3 = s1f3pressout OUTPUT W
<L [4]
    <U2 [1]> = Press1
    <U2 [1]> = Press2
    <U2 [1]> = Press3
    <U2 [1]> = Press4
>.

S1F3 = s1f3Specific OUTPUT W
<V>=DATA.

S1F3 =s1f3ASM120TRcpPara OUTPUT W
<L[1]
<U2 [1]>=Data0
>.
****************************************************************************
* S1F4    Selected Equipment Status                       E -> H     *
****************************************************************************
S1F4 = s1f4statein INPUT 
<L [3]  
    <I2 [1]> = processState
    <A [MAX 50]> = ppName  
    <U1 [1]> = controlState
>.
S1F4 = s1f4in INPUT 
  <V>=RESULT.
****************************************************************************
* S1F11    Status Values Namelists Request  (svnr)              E <- H     *
****************************************************************************
S1F11 =s1f11out OUTPUT W
  <L [0]>.
****************************************************************************
* S1F12    Status Values Namelists Reply  (svnrr)                   E ->H  *
****************************************************************************
S1F12 =s1f12in INPUT 
     <V> = RESULT.
****************************************************************************
* S1F13   Establish Communications Request (CR)                H <-> E     *
****************************************************************************
S1F13 =s1f13in INPUT W
<L [2]
    <A [MAX 6] >                     = Mdln
    <A [MAX 6] >                     = SoftRev
>.
S1F13 = s1f13out OUTPUT W
<L [0]>.
****************************************************************************
* S1F14   Establish Communications Request Acknowledge (CR)      H <-> E   *
****************************************************************************
S1F14 = s1f14in INPUT
  <L [2]
    <B [1] >                          = AckCode
    <L [2]
      <A [MAX 6] >                    = Mdln
      <A [MAX 6] >                    = SoftRev
    >
  >.
S1F14 = s1f14out OUTPUT
<L [2]
      <B [1] >                        = AckCode
      <L [0] >
>.

****************************************************************************
* S1F15   Request OFF-LINE                H -> E     *
****************************************************************************
S1F15 = s1f15out OUTPUT W.
 
****************************************************************************
* S1F16   OFF-LINE ACK                H <- E     *
****************************************************************************
S1F16 INPUT = s1f16in
<B [1]> = AckCode.
 
****************************************************************************
* S1F17   Request ON-LINE                H -> E     *
****************************************************************************
S1F17 = s1f17out OUTPUT W.

****************************************************************************
* S1F18   ON-LINE ACK                H <- E     *
****************************************************************************
S1F18 INPUT = s1f18in
<B [1]>=AckCode.
****************************************************************************
* S6F11  Event Report Send (ERS)                               E -> H      *
****************************************************************************
S6F11 = s6f11incommon2  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [2]
             <A [MAX 60]>                     =Data1
             <A [MAX 60]>                     =Data2         
           >                    
         >
       >
 >.
S6F11 = s6f11incommon3  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [3]
             <A [MAX 60]>                     =Data1
            <U4 [1] >                        = Data2
             <A [MAX 60]>                     =Data3         
           >                    
         >
       >
 >.

S6F11  =s6f11equipstatuschange INPUT W 
<L [3]
	<U4[1] >  = DataID
	<U4[1] >   = CollEventID
	<L [1]
		<L [2]
			<U4[1] >= ReportId
			<L [3]
				<I2[1] > =EquipStatus
				<I2[1] > =Data2
				<I4[1] > =Data3
			>
		>
	>
>.
S6F11 =s6f11incommon7 INPUT W 
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] >= ReportId
			<L [4]
				<A[0]> =Data1
				<A[MAX 30] > =Data2
				<A[MAX 30] > =Data3
				<A[MAX 10] > =Data4
			>
		>
	>
>.
S6F11 =s6f11incommon9 INPUT W 
<L [3]
	<U4[1] >= DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] >= ReportId
			<L [2]
				<A[MAX 50] > =Data1
				<I4[1] > =Data2
			>
		>
	>
>. 
S6F11 = s6f11incommon10 INPUT W 
<L [3]
	<U4[1] >= DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] >= ReportId
			<L [4]
				<I4[1] > =Data1
				<A[MAX 80]>=Data2
				<U1[1] >=Data3
				<Boolean[1] >=Data4
			>
		>
	>
>. 
S6F11 = s6f11incommon11 INPUT W
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [5]
				<A[MAX 50]> =Data1
				<A[MAX 50]> =Data2
				<A[MAX 50]> =Data3
				<I4[1] > =Data4
				<A[MAX 50]> =Data5
			>
		>
	>
>. 

S6F11  =s6f11equipstate INPUT W 
<L [3]
	<U4[1] >    = DataID
	<U4[1] >    = CollEventID
	<L [1]
		<L [2]
			<U4[1] >    = ReportId
			<L [2]
				<A[MAX 50] >    =Data1
				<I2[1] >        =Data2
			>
		>
	>
>.
S6F11 = s6f11ppselectfinish INPUT W 
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4 [1] > = ReportId
			<L [1]                          
				<A [MAX 100]> = PPExecName				
			>
		>
	>
>.

S6F11 = s6f11incommon12 INPUT W
<L [3]
	<U4[1] > = DataID
 	<U4[1] > = CollEventID
	<L [4]
		<L [2]
			<U4[1] > = ReportId0
			<L [2]
				<A[MAX 100] > = Data0
				<A[MAX 100] > = Data1
			>
		>
		<L [2]
			<U4[1] > = ReportId1
			<L [2]
				<F4[1] > = Data2
				<F4[1] > = Data3
			>
		>
		<L [2]
			<U4[1] > = ReportId2
			<L [4]
				<F4[1] > = Data4
				<F4[1] > = Data5
				<F4[1] > = Data6
				<F4[1] > = Data7
			>
		>
		<L [2]
			<U4[1] > = ReportId3
			<L [2]
				<F4[1] > = Data8
				<F4[1] > = Data9
			>
		>
	>
>. 

S6F11 = s6f11incommon13 INPUT W
<L [3]
	<U4[1] > = DataID
	<U4[1] >  = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [3]
				<A[MAX 100] > = Data0
				<A[MAX 100] > = Data1
				<A[MAX 100] > = Data2
			>
		>
	>
>.

S6F11 = s6f11incommon14 INPUT W
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [2]
				<A[MAX 100] > = Data0
				<F4[1] > = Data1
			>
		>
	>
>.

S6F11 = s6f11incommon15 INPUT W
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [4]
				<I4[1] > = Data0
				<U1[1] > = Data1
				<A[MAX 100] > = Data2
				<L [1]
					<U4[1] > = Data3
				>
			>
		>
	>
>.

S6F11 = s6f11incommon16 INPUT W
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [3]
				<I4[1] > = Data0
				<A[MAX 100] > = Data1
				<L [0]
				>
			>
		>
	>
>.


S6F11 = s6f11incommon17 INPUT W
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [2]
				<I4[1] > = Data1
				<I4[1] > = Data2
			>
		>
	>
>.
S6F11 = s6f11incommon18 INPUT W
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [10]
				<V>=DATA1
				<V>=DATA2
				<V>=DATA3
				<V>=DATA4
				<V>=DATA5
				<V>=DATA6
				<V>=DATA7
				<V>=DATA8
				<V>=DATA9
				<V>=DATA10
			>
		>
	>
>. 
S6F11 = s6f11incommon19 INPUT W
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [2]	
			<V> = DATA
			<V> = DATA1

	>
>. 

****************************************************************************
* S6F12  Event Report Acknowledge (ERA)                        H -> E      *
****************************************************************************
S6F12 OUTPUT = s6f12out
  <B [1]>  = AckCode.
S6F12 Input = s6f12in
  <B [1]>     = AckCode.
****************************************************************************
* S9F1  Unrecognized Device Id (UDN)                           E -> H      *
****************************************************************************

S9F1 OUTPUT = s9f1out
  <B [10] >                      = BadDevHead.
 
S9F1 INPUT = s9f1input
  <B [10] >                      = BadDevHead.

****************************************************************************
* S9F3  Unrecognized Stream Type (USN)                         E -> H      *
****************************************************************************

S9F3 OUTPUT = s9f3out
  <B [10] >                      = BadStreamHead.

S9F3 INPUT = s9f3input
  <B [10] >                      = BadStreamHead.

****************************************************************************
* S9F5  Unrecognized Function Type (UFN)                       E -> H      *
****************************************************************************

S9F5 OUTPUT = s9f5out
  <B [10] >                      = BadFuncHead.

S9F5 INPUT = s9f5input
  <B [10] >                      = BadFuncHead.

****************************************************************************
* S9F7  Illegal Data (IDN)                                     E -> H      *
****************************************************************************

S9F7 OUTPUT = s9f7out
  <B [10] >                      = IllDataHead.

 S9F7 INPUT = s9f7input
  <B [10] >                      = IllDataHead.

****************************************************************************
* S9F9  Transaction Timer Timeout (TTN)                        E -> H      *
****************************************************************************

S9F9 OUTPUT = s9f9out
  <B [10] >                      = TranTOHead.

 S9F9 INPUT = s9f9input
  <B [10] >                      = TranTOHead.

****************************************************************************
* S9F11  Data Too Long (DLN)                                   E -> H      *
****************************************************************************

S9F11 OUTPUT = s9f11out
  <B [10] >                      = DataLongHead.

S9F11 INPUT = s9f11input
  <B [10] >                      = DataLongHead.

****************************************************************************
* S10F1  Terminal request                                     E -> H      *
****************************************************************************
S10F1  = s10f1in  INPUT W
<L [2]
        <V>      = TID
        <A [MAX 1000]>   = TEXT
>.
S10F2 output = s10f2out
 <B [1]>        = AckCode
.
 
S10F3 output = s10f3out W
<L [2]
        <B[1]>      = TID
        <A [MAX 1000]>   = TEXT
>.

S10F4 INPUT = s10f4in
 <B [1]>        = AckCode
. 
****************************************************************************
* S2F13    Equipment Constant Request                          H -> E      *
****************************************************************************

S2F13 =s2f13out OUTPUT W
< L [0]>.

****************************************************************************
* S2F14    Equipment Constant Data                          H<- E      *
****************************************************************************

S2F14 =s2f14in INPUT 
< V >=RESULT.

****************************************************************************
* S2F29   Equipment Canstant Namelist Request                   H- >E      *
****************************************************************************

S2F29 =s2f29out OUTPUT W 
< L [0]>.
S2F29 =s2f29oneout OUTPUT W 
< L [1]
    <U4[1]>=ECID
>.

****************************************************************************
* S2F30     Equipment Canstant Namelist Reply                    H- >E      *
****************************************************************************

S2F30 =s2f30in INPUT 
<V> = RESULT.

****************************************************************************
* S2F33    Define Report (DR)                                  H -> E      *
****************************************************************************

S2F33 OUTPUT = s2f33out W
<L [2]
    <U2 [1]>=DataID
    <L[1]
        <L [2]
            <U2 [1]>              =ReportID
            <L[1]
               <U2 [1]>          = VariableID            
            >
        >        
    >
>.
S2F33 OUTPUT = s2f33eqpstate W
<L [2]
    <U2 [1]>=DataID
    <L[1]
        <L [2]
            <U2 [1]>            =ReportID
            <L[2]              
               <U2 [1]>         = EquipStatusID 
               <U2 [1]>         = PPExecNameID     
              
            >
        >        
    >
>.
****************************************************************************
* S2F34 Define Report Acknowledge (DRA)                        H <- E      *
****************************************************************************

S2F34 INPUT = s2f34in 

<B[1]>=AckCode.

****************************************************************************
* S2F35    Link Event Report (LER)                             H -> E      *
****************************************************************************
S2F35 OUTPUT = s2f35out W
<L[2]
    <U2 [1]> = DataID
     <L[1]
        <L [2]
            <U2 [1]> = CollEventID
            <L[1]
                <U2 [1]> = ReportID
            >
        >
    >
>.

****************************************************************************
* S2F36   Link Event Report Acknowledge(LERA)                  E -> H      *
****************************************************************************
S2F36 INPUT = s2f36in 
<B [1]> = AckCode.
****************************************************************************
* S2F37   Enable / Disable Event Report (EDER)                 H -> E      *
****************************************************************************

S2F37 OUTPUT = s2f37out W
<L[2]
    <BOOLEAN [1]> = Booleanflag
    <L[1]
        <U4 [1]> = CollEventId
    >
>.

S2F37 OUTPUT = s2f37outAll W
<L[2]
    <BOOLEAN [1]> = Booleanflag
    <L[0]
    >
>.
****************************************************************************
* S2F38  Enable / Disable Event Report Acknowledge (EERA)      E -> H      *
****************************************************************************

S2F38 INPUT = s2f38in 
<B [1]> = AckCode.
****************************************************************************
* S5F1 Alarm Report Send                    E -> H      *
****************************************************************************
S5F1 = s5f1in INPUT W
<L[3]
    <B [1]> = ALCD
    <U4 [1]> = ALID
    <A [MAX 100]> = ALTX
>.
****************************************************************************
* S5F2 Alarm Report ACK                    E -> H      *
****************************************************************************
S5F2 = s5f2out OUTPUT 
    <B [1]> = AckCode.
  
****************************************************************************
* S5F3  Enable Alarm Send                    E <- H      *
****************************************************************************
S5F3 OUTPUT = s5f3allout  W 
<L [2]
    < B [1] > = ALED
    < L [0]>
>.
S5F3 = s5f3out OUTPUT W 
<L[2]
    <B [1]> = ALED
    <U4 [1]> = ALID   
>.
****************************************************************************
* S5F4 Enable Alarm Report ACK                    E -> H         *
****************************************************************************
S5F4 = s5f4in INPUT 
    <B [1]> = AckCode.  
****************************************************************************
* S5F5 List Alarm Request                    E <- H         *
****************************************************************************
S5F5 = s5f5out OUTPUT W
    <L [0]> .  
****************************************************************************
* S5F7 List Enableed Alarm Request                    E <- H         *
****************************************************************************
S5F7 = s5f7out OUTPUT W
    <L [0]> .  
****************************************************************************
* S5F8 List Enableed Alarm Respones                    E <- H         *
****************************************************************************
S5F8 = s5f8in INPUT 
    <V> = RESULT.  
****************************************************************************
* S7F1    Process Program Load Inquire                          E<->H      *
****************************************************************************
S7F1  = s7f1out OUTPUT W
<L[2]
    <A [MAX 100] >             = ProcessprogramID
    <U4 [1]>                   = Length
>.
****************************************************************************
* S7F2   Process Program Load Grant                          E<->H      *
****************************************************************************
S7F2 INPUT = s7f2in
<B [1]> = PPGNT.
****************************************************************************
* S7F3   Process Program Send                                E<->H      *
****************************************************************************
S7F3  = s7f3out OUTPUT W
<L[2]
    <A [MAX 100] >               =ProcessprogramID 
    <V>              =Processprogram
>.

* <B [MAX 10000000]>           =Processprogram
****************************************************************************
* S7F4   Process Program Acknowledge                          E<->H      *
****************************************************************************
S7F4 OUTPUT = s7f4out
<B [1]> = AckCode.

S7F4 INPUT =s7f4in
<B [1]> = AckCode.
****************************************************************************
* S7F5  Process Program Request                                 E<->H      *
****************************************************************************
S7F5 OUTPUT = s7f5out W
<A [MAX 100] >= ProcessprogramID.


****************************************************************************
* S7F6  Process Program Data                                    H<->E      *
****************************************************************************

S7F6 = s7f6in INPUT 
<L[2]
    <A [MAX 100] >          =  ProcessprogramID 
    <V>                     = Processprogram
>.
S7F6 = s7f6zeroin INPUT 
<L[0]   
>.
S7F6 = s7f6input INPUT .
****************************************************************************
* S7F17 Delete Process Program Send                              H->E      *
****************************************************************************
S7F17  = s7f17out OUTPUT W
<L [1]
    <A [MAX 100]>                = ProcessprogramID 
>.
****************************************************************************
* S7F18 Delete Process Program Acknowledge                       E->H      *
****************************************************************************
S7F18 INPUT = s7f18in
<B [1]> = AckCode.

****************************************************************************
* S7F19 Current EPPD Request                                     H->E      *
****************************************************************************
S7F19  = s7f19out OUTPUT W.

****************************************************************************
* S7F20 Current EPPD Data                                        E->H      *
****************************************************************************
S7F20 INPUT = s7f20in
    <V>  = EPPD.
****************************************************************************
*S2F41 Host Command Send(HCS)                                         H->E*
****************************************************************************
S2F41  = s2f41out OUTPUT W
<L[2]
    <A[MAX 20]> = Remotecommand
    <L[1]
        <L[2]
            <A[MAX 20]> = Commandparametername
            <A[MAX 30]> = Commandparametervalue
        >
    >
>.
S2F41  = s2f41zeroout OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L [0]       
    >
>.
S2F41  = s2f41outPPSelect OUTPUT W
<L[2]
    <A 'PP_SELECT'> 
    <L[1]
        <L[2]
            <A 'PPID'> 
            <A [MAX 100]> = PPID
        >    
    >
>.
****************************************************************************
*S2F42 Host Command Send(HCS)                                         H<-E*
****************************************************************************

S2F42 = s2f42zeroin INPUT 
<L [2]
    <B [1] > = HCACK 
    <L[0]
    >
>.

S2F42 INPUT = s2f42in
<L [2]
	<B[1] >= HCACK
	<L [1]
		<L [2]
			<A[MAX 20] >= DATA1
			<B[1] >= DATA2
		>
	>
>.