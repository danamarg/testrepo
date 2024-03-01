import java.util.ArrayList;
import java.util.List;

public class TrajectoryUtils {
    
    def path_to_trajectory(
        path: Union[Sequence[Vector],Trajectory,RobotTrajectory],
        velocities: _VELOCITIES_OPTIONS = 'auto',
        timing: Union[_TIMING_OPTIONS,List[float],MetricType]= 'limited',
        smoothing: str='spline',
        stoptol: Optional[float] = None,
        vmax: Union[str,float,Vector] = 'auto',
        amax: Union[str,float,Vector] = 'auto',
        speed: float = 1.0,
        dt: float = 0.01,
        startvel: float = 0.0,
        endvel: float = 0.0,
        verbose: int = 0
    ) -> Trajectory:
    Converts an untimed path to a timed trajectory.

    The resulting trajectory passes through each of the milestones **without
    stopping**, except for "stop" milestones.  Stop milestones by default are
    only the first and last milestone, but if ``stoptol`` is given, then the
    trajectory will be stopped if the curvature of the path exceeds this value.

    The first step is to assign each segment a 'distance' d[i] suggesting how
    much time it would take to traverse that much spatial distance.  This
    distance assignment method is controlled by the ``timing`` parameter.

    The second step is to smooth the spline, if smoothing='spline' is given
    (default).

    The third step creates the trajectory, assigning the times and velocity
    profile as specified by the ``velocities`` parameter.  ``velocities``
    dictates how the overall velocity profile behaves from beginning to end,
    and basically, each profile gradually speeds up and slows down.  The
    path length L = :math:`\sum_{i=1}^n d[i]` determines the duration T of
    the trajectory, as follows:

    - For constant velocity profiles, T=L. 
    - For trapezoidal, triangular, parabolic, and cosine, T = sqrt(L). 
    - For minimum-jerk, T = L^(1/3). 

    The fourth step is to time scale the result to respect limits velocity /
    acceleration limits, if timing=='limited' or speed=='limited'.

    The fifth step is to time scale the result by speed.

    .. note::

        There are only some meaningful combinations of arguments:

        - velocities='auto'; timing='limited': a generates a timed spline using a
          heuristic and then revises it to respect velocity and acceleration limits.

          The limited timing heuristic works best when the milestones are widely
          spaced.

          Be sure to specify vmax and amax if you don't have a RobotTrajectory.

        - velocities='auto', 'trapezoidal', 'triangular', 'parabolic', 'cosine', or
          'minimum-jerk';
          timing='L2', 'Linf', 'robot', 'sqrt-L2', 'sqrt-Linf', or 'sqrt-robot':
          an entirely heuristic approach. 

          The sqrt values lead to somewhat better tangents for smoothed splines with
          nonuniform distances between milestones.

          In these cases, vmax and amax are ignored.

        - If path uses non-Euclidean interpolation, then smoothing=None should be
          provided.  Smoothing is not yet supported for non-Euclidean spaces (e.g.,
          robots with special joints, SO3, SE3).

    Args:
        path: a list of milestones, a trajectory, or a RobotTrajectory.  In the
            latter cases, if durations = 'path' then the durations are extracted
            from the trajectory's timing.

        velocities (str, optional): the manner in which velocities are assigned
            along the path. Can be:

            - 'auto' (default): if timing is 'limited', this is equivalent to
              'constant'. Otherwise, this is equivalent to 'trapezoidal'.
            - 'trapezoidal': a trapezoidal velocity profile with max
              acceleration and velocity.  If timing is 'limited', the velocity
              max is determined by vmax.  Otherwise, the ramp
              up proceeds for 1/4 of the time, stays constant 1/2 of the time,
              and then ramps down for 1/4 of the time.
            - 'constant': the path is executed at fixed constant velocity
            - 'triangular': velocities are ramped up for 1/2 of the duration
              and then ramped back down.
            - 'parabolic': a parabolic curve (output is a Hermite spline)
            - 'cosine': velocities follow (1-cosine)/2
            - 'minimum-jerk': minimum jerk velocities
            - 'optimal': uses time scaling optimization. NOT IMPLEMENTED YET

        timing (optional): affects how path timing between milestones is
            normalized. Valid options are:

            - 'limited' (default): uses the vmax, amax variables along with
              the velocity profile to dynamically determine the duration
              assigned to each segment.
            - 'uniform': base timing between milestones is uniform
              (1/(\|path\|*speed))
            - 'path': only valid if path is a Trajectory object.  Uses the
              timing in path.times as the base timing.
            - 'L2': base timing is set proportional to L2 distance between
              milestones
            - 'Linf': base timing is set proportional to L-infinity distance
              between milestones
            - 'robot': base timing is set proportional to robot's distance
              function between milestones
            - 'sqrt-L2', 'sqrt-Linf', or 'sqrt-robot': base timing is set
              proportional to the square root of the L2, Linf, or robot
              distance between milestones
            - a list or tuple: the base timing is given in this list
            - callable function f(a,b): sets the normalization to the function
              f(a,b).

        smoothing (str, optional): if 'spline', the geometric path is first
            smoothed before assigning times.  Otherwise, the geometric path
            is interpreted as a piecewise linear path.

        stoptol (float, optional): determines how start/stop segments are
            determined.  If None, the trajectory only pauses at the start and
            end of the path.  If 0, it pauses at every milestone. Otherwise,
            it pauses if the curvature at the milestone exceeds stoptol.

        vmax (optional): only meaningful if timing=='limited'. Can be:

            - 'auto' (default): either 1 or the robot's joint velocity limits
              if a RobotTrajectory is provided
            - a positive number: the L2 norm of the derivative of the result
              trajectory is limited to this value
            - a list of positive floats: the element-wise derivative of the
              result trajectory is limited to this value

        amax (optional): only meaningful if timing=='limited'. Can be:

            - 'auto' (default): either 4 or the robot's joint acceleration
              limits if a RobotTrajectory is provided
            - a positive number: the L2 norm of the acceleration of the result
              trajectory is limited to this value
            - a list of positive floats: the element-wise acceleration of the
              result trajectory is limited to this value.

        speed (float or str, optional): if a float, this is a speed multiplier
            applied to the resulting trajectory.  This can also be 'limited',
            which applies the velocity and acceleration limits.

        dt (float, optional): the resolution of the resulting trajectory. 
            Default 0.01.

        startvel (float, optional): the starting velocity of the path, given as
            a multiplier of path[1]-path[0].  Must be nonnegative. 

            Note: might not be respected for some velocity profiles.

            .. warning::
                NOT IMPLEMENTED YET

        endvel (float, optional): the ending velocity of the path, given as a
            multiplier of path[-1]-path[-2].  Must be nonnegative.

            Note: might not be respected for some velocity profiles.

            .. warning::
                NOT IMPLEMENTED YET

        verbose (int, optional): if > 0, some debug printouts will be given.

    Returns:
        A finely-discretized, timed trajectory that is C1 continuous
        and respects the limits defined in the arguments.
    """
    assert dt > 0.0,"dt has to be positive"
    if vmax == 'auto' and (timing == 'limited' or speed == 'limited'):
        if isinstance(path,RobotTrajectory):
            vmax = path.robot.getVelocityLimits()
        else:
            vmax = 1.0
    if amax == 'auto' and (timing == 'limited' or speed == 'limited'):
        if isinstance(path,RobotTrajectory):
            amax = path.robot.getAccelerationLimits()
        else:
            amax = 4.0
    if isinstance(speed,(int,float)) and speed != 1.0:
        if not (speed > 0):
            raise ValueError("Invalid value for speed, must be positive")
        dt *= speed
        startvel /= speed
        endvel /= speed
                    
    milestones = path
    if isinstance(path,Trajectory):
        milestones = path.milestones

    _durations = None
    if isinstance(timing,(list,tuple)):
        _durations = timing
    elif callable(timing):
        _durations = [timing(a,b) for a,b in zip(milestones[:-1],milestones[1:])]
    else:
        if isinstance(path,Trajectory):
            if timing == 'path':
                _durations = [(b-a) for a,b in zip(path.times[:-1],path.times[1:])]
        if _durations is None:
            if timing == 'limited':
                if hasattr(vmax,'__iter__'):
                    if not all(v >= 0 for v in vmax):
                        raise ValueError("Invalid value for vmax, must be positive")
                else:
                    if not vmax >= 0:
                        raise ValueError("Invalid value for vmax, must be positive")

                if hasattr(amax,'__iter__'):
                    if not all(v >= 0 for v in amax):
                        raise ValueError("Invalid value for amax, must be positive")
                else:
                    if not amax >= 0:
                        raise ValueError("Invalid value for amax, must be positive")
                _durations = [0.0]*(len(milestones)-1)
                for i in range(len(milestones)-1):
                    q,n = milestones[i],milestones[i+1]
                    if i == 0: p = q
                    else: p = milestones[i-1]
                    if i+2 == len(milestones): nn = n
                    else: nn = milestones[i+2]
                    if isinstance(path,Trajectory):
                        v = vectorops.mul(path.difference_state(p,n,0.5,1.0),0.5)
                        a1 = vectorops.sub(path.difference_state(q,n,0.,1.),path.difference_state(p,q,1.,1.))
                        a2 = vectorops.sub(path.difference_state(n,nn,0.,1.),path.difference_state(q,n,1.,1.))
                    else:
                        v = vectorops.mul(vectorops.sub(n,p),0.5)
                        a1 = vectorops.madd(vectorops.add(p,n),q,-2.0)
                        a2 = vectorops.madd(vectorops.add(q,nn),n,-2.0)
                    if hasattr(vmax,'__iter__'):
                        for j,(x,lim) in enumerate(zip(v,vmax)):
                            if abs(x) > lim*_durations[i]:
                                _durations[i] = abs(x)/lim
                                #print("Segment",i,"limited on axis",j,"path velocity",x,"limit",lim)
                    else:
                        _durations[i] = vectorops.norm(v)/vmax
                    if hasattr(amax,'__iter__'):
                        if i > 0:
                            for j,(x,lim) in enumerate(zip(a1,amax)):
                                if abs(x) > lim*_durations[i]**2:
                                    _durations[i] = math.sqrt(abs(x)/lim)
                                    #print("Segment",i,"limited on axis",j,"path accel",x,"limit",lim)
                        if i+2 < len(milestones):
                            for j,(x,lim) in enumerate(zip(a2,amax)):
                                if abs(x) > lim*_durations[i]**2:
                                    _durations[i] = math.sqrt(abs(x)/lim)
                                    #print("Segment",i,"limited on axis",j,"outgoing path accel",x,"limit",lim)
                    else:
                        if i > 0:
                            n = vectorops.norm(a1)
                            if n > amax*_durations[i]**2:
                                _durations[i] = math.sqrt(n/amax)
                        if i+2 < len(milestones):
                            n = vectorops.norm(a2)
                            if n > amax*_durations[i]**2:
                                _durations[i] = math.sqrt(n/amax)
            else:
                durationfuncs = dict()
                durationfuncs['L2'] = vectorops.distance
                durationfuncs['Linf'] = lambda a,b:max(abs(u-v) for (u,v) in zip(a,b))
                durationfuncs['sqrt-L2'] = lambda a,b:math.sqrt(vectorops.distance(a,b))
                durationfuncs['sqrt-Linf'] = lambda a,b:math.sqrt(max(abs(u-v) for (u,v) in zip(a,b)))
                if hasattr(path,'robot'):
                    durationfuncs['robot'] = path.robot.distance
                    durationfuncs['sqrt-robot'] = lambda a,b:math.sqrt(path.robot.distance(a,b))
                assert timing in durationfuncs,"Invalid duration function specified, valid values are: "+", ".join(list(durationfuncs.keys()))
                timing = durationfuncs[timing]
                _durations = [timing(a,b) for a,b in zip(milestones[:-1],milestones[1:])]
    assert _durations is not None,"Hmm... didn't assign durations properly?"
    if verbose >= 1:
        print("path_to_trajectory(): Segment durations are",_durations)
    #by this time we have all milestones and durations
    if stoptol is not None:
        splits = [0]
        #split the trajectory then reassemble it
        for i in range(1,len(milestones)-1):
            prev = milestones[i-1]
            q = milestones[i]
            next = milestones[i+1]
            acc = vectorops.madd(vectorops.add(prev,next),q,-2.0)
            if vectorops.norm(acc) > stoptol*(_durations[i]*_durations[i-1]):
                splits.append(i)
        splits.append(len(milestones)-1)
        if len(splits) > 2:
            if verbose >= 1:
                print("path_to_trajectory(): Splitting path into",len(splits)-1,"segments, starting and stopping between")
            res = None
            for i in range(len(splits)-1):
                a,b = splits[i],splits[i+1]
                segmentspeed = (1.0 if isinstance(speed,(int,float)) else speed)
                traj = path_to_trajectory(milestones[a:b+1],velocities,timing,smoothing,
                    None,vmax,amax,
                    segmentspeed,dt)
                if res is None:
                    res = traj
                else:
                    if res.milestones[-1] != traj.milestones[0]: #may have hermite spline interpolation problems
                        res.times.append(res.times[-1])
                        res.milestones.append(traj.milestones[0])
                    res = res.concat(traj,relative=True)
            if isinstance(speed,(int,float)) and speed != 1.0:
                res.times = vectorops.mul(res.times,1.0/speed)
            return res
    #canonical case:
    #milestones and _durations are lists
    #start and stop at beginning / end
    #speed = 1 or 'limited'
    normalizedPath = Trajectory()
    if isinstance(path,RobotTrajectory):
        normalizedPath = RobotTrajectory(path.robot)
    normalizedPath.milestones = milestones
    normalizedPath.times = [0]
    totaldistance = 0
    for d in _durations:
        totaldistance += d
        normalizedPath.times.append(totaldistance)

    if startvel != 0.0 or endvel != 0.0:
        print("path_to_trajectory(): WARNING: respecting nonzero start/end velocity not implemented yet")

    if smoothing == 'spline':
        hpath = HermiteTrajectory()
        hpath.makeSpline(normalizedPath)
        normalizedPath = hpath

    #print("path_to_trajectory(): Total distance",totaldistance)
    if totaldistance == 0.0:
        return normalizedPath
    finalduration = totaldistance
    evmax = 1
    eamax = 0
    if velocities == 'auto':
        if timing == 'limited':
            velocities = 'constant'
        else:
            velocities = 'trapezoidal'
    if velocities == 'constant':
        easing = lambda t: t
        evmax = 1.0
        eamax = 0.0
    elif velocities == 'trapezoidal' or velocities == 'triangular':
        easing = lambda t: 2*t**2 if t < 0.5 else 1.0-(2*(1.0-t)**2)
        evmax = 2.0
        eamax = 2.0
        if velocities == 'trapezoidal' and timing != 'limited':
            #ramp up c t^2 until 0.25
            #velocity 2 c t, ending velocity c/2, ending point c/16
            #continue for 0.5, ending point c/16 + c/4
            #ramp down for distance c/16, total distance c/8 + c/4 = 1 => c = 8/3
            easing = lambda t: 8.0/3.0*t**2 if t < 0.25 else (1.0-(8.0/3.0*(1.0-t)**2) if t > 0.75 else 1.0/6.0 + 4.0/3.0*(t-0.25))
        finalduration = math.sqrt(totaldistance)
    elif velocities == 'cosine':
        easing = lambda t: 0.5*(1.0-math.cos(t*math.pi))
        evmax = math.pi*0.5  #pi/2 sin (t*pi)
        eamax = math.pi**2*0.5   #pi**2/2 cos(t*pi)
        finalduration = math.sqrt(totaldistance)
    elif velocities == 'parabolic':
        easing = lambda t: -2*t**3 + 3*t**2
        evmax = 1.5  #-6t*2 + 6t
        eamax = 6    #-12t + 6
        finalduration = math.sqrt(totaldistance)
    elif velocities == 'minimum-jerk':
        easing = lambda t: 10.0*t**3 - 15.0*t**4 + 6.0*t**5 
        evmax = 15*0.25   #30t^2 - 60t*3 + 30t^4 => 1/4*(30 - 30 + 30/4)= 30/8
        t = 1.0 + math.sqrt(1.0/3.0)
        eamax = 30*t - 45*t**2 + 15*t**3         #60t - 180t*2 + 120t^3 => max at 1/6 - t + t^2 = 0 => t = (1 +/- sqrt(1 - 4/6))/2 = 1/2 +/- 1/2 sqrt(1/3)
                                                 #30(1 + sqrt(1/3)) - 45(1 + sqrt(1/3))^2 + 15(1 + sqrt(1/3))^3 
        finalduration = math.pow(totaldistance,1.0/3.0)
    else:
        raise NotImplementedError("Can't do velocity profile "+velocities+" yet")
    if timing == 'limited':
        #print("Easing velocity max",evmax,"acceleration max",eamax)
        #print("Velocity and acceleration-limited segment distances",_durations)
        #print("Total distance traveled",totaldistance)
        finalduration = totaldistance*evmax
        #y(t) = p(L*e(t/T))
        #y'(t) = p'(L*e(t)/T)*e'(t) L/T 
        #y''(t) = p''(e(t))*e'(t)^2(L/T)^2 + p'(e(t))*e''(t) (L/T)^2
        #assume |p'(u)| <= vmax, |p''(u)| <= amax
        #set T so that |p'(u)| e'(t) L/T <= |p'(u)| evmax L/T  <= vmax evmax L/T <= vmax
        #set T so that |p''(u)| evmax^2 (L/T)^2 + |p'(u)|*e''(t) (L/T)^2 <= (amax evmax^2 + vmax eamax) (L/T)^2 <= amax
        #T >= L sqrt(evmax^2 + vmax/amax eamax)
        if finalduration < totaldistance*math.sqrt(evmax**2 + eamax):
            finalduration = totaldistance*math.sqrt(evmax**2 + eamax)
        if verbose >= 1:
            print("path_to_trajectory(): Setting first guess of path duration to",finalduration)
    res = normalizedPath.constructor()()
    if finalduration == 0:
        if verbose >= 1:
            print("path_to_trajectory(): there is no movement in the path, returning a 0-duration path")
        res.times = [0.0,0.0]
        res.milestones = [normalizedPath.milestones[0],normalizedPath.milestones[0]]
        return res
    N = int(math.ceil(finalduration/dt))
    assert N > 0
    dt = finalduration / N
    res.times=[0.0]*(N+1)
    res.milestones = [None]*(N+1)
    res.milestones[0] = normalizedPath.milestones[0][:]
    dt = finalduration/float(N)
    #print(velocities,"easing:")
    for i in range(1,N+1):
        res.times[i] = float(i)/float(N)*finalduration
        u = easing(float(i)/float(N))
        #print(float(i)/float(N),"->",u)
        res.milestones[i] = normalizedPath.eval_state(u*totaldistance)
    if timing == 'limited' or speed == 'limited':
        scaling = 0.0
        vscaling = 0.0
        aLimitingTime = 0
        vLimitingTime = 0
        for i in range(N):
            q,n = res.waypoint(res.milestones[i]),res.waypoint(res.milestones[i+1])
            if i == 0: p = q
            else: p = res.waypoint(res.milestones[i-1])
            if isinstance(path,Trajectory):
                v = path.difference_state(p,n,0.5,dt*2.0)
                a = vectorops.sub(path.difference_state(q,n,0.,dt),path.difference_state(p,q,1.,dt))
                a = vectorops.div(a,dt)
            else:
                v = vectorops.div(vectorops.sub(n,p),dt*2.0)    
                a = vectorops.div(vectorops.madd(vectorops.add(p,n),q,-2.0),dt**2)
            if not hasattr(vmax,'__iter__'):
                n = vectorops.norm(v)
                if n > vmax*scaling:
                    #print("path segment",i,"exceeded scaling",scaling,"by |velocity|",n,' > ',vmax*scaling)
                    vscaling = n/vmax
                    vLimitingTime = i
            else:
                for x,lim in zip(v,vmax):
                    if abs(x) > lim*vscaling:
                        #print("path segment",i,"exceeded scaling",scaling,"by velocity",x,' > ',lim*scaling)
                        #print("Velocity",v)
                        vscaling = abs(x)/lim
                        vLimitingTime = i
            if i == 0:
                continue
            if not hasattr(amax,'__iter__'):
                n = vectorops.norm(a)
                if n > amax*scaling**2:
                    #print("path segment",i,"exceeded scaling",scaling,"by |acceleration|",n,' > ',amax*scaling**2)
                    scaling = math.sqrt(n/amax)
                    aLimitingTime = i
            else:
                for x,lim in zip(a,amax):
                    if abs(x) > lim*scaling**2:
                        #print("path segment",i,"exceeded scaling",scaling,"by acceleration",x,' > ',lim*scaling**2)
                        #print(p,q,n)
                        #print("Velocity",v)
                        #print("Previous velocity",path.difference(p,q,1.,dt))
                        scaling = math.sqrt(abs(x)/lim)
                        aLimitingTime = i
        if verbose >= 1:
            print("path_to_trajectory(): Base traj exceeded velocity limit by factor of",vscaling,"at time",res.times[vLimitingTime]*max(scaling,vscaling))
            print("path_to_trajectory(): Base traj exceeded acceleration limit by factor of",scaling,"at time",res.times[aLimitingTime]*max(scaling,vscaling))
        if velocities == 'trapezoidal':
            #speed up until vscaling is hit
            if vscaling < scaling:
                if verbose >= 1:
                    print("path_to_trajectory(): Velocity maximum not hit")
            else:
                if verbose >= 1:
                    print("path_to_trajectory(): TODO: fiddle with velocity maximum.")
                scaling = max(vscaling,scaling)
                res.times = [t*scaling for t in res.times]
        else:
            scaling = max(vscaling,scaling)
        if verbose >= 1:
            print("path_to_trajectory(): Velocity / acceleration limiting yields a time expansion of",scaling)
        res.times = vectorops.mul(res.times,scaling)
    if isinstance(speed,(int,float)) and speed != 1.0:
        res.times = vectorops.mul(res.times,1.0/speed)
    return res


def execute_path(
        path: List[Vector],
        controller: Union['SimRobotController','RobotInterfaceBase'],
        speed: float = 1.0,
        smoothing: Optional[_SMOOTHING_OPTIONS] = None,
        activeDofs: Optional[List[Union[int,str]]] = None
    ):
    """Sends an untimed trajectory to a controller.

    If smoothing = None, the path will be executed as a sequence of go-to
    commands, starting and stopping at each milestone.  Otherwise, it will
    be smoothed somehow and sent to the controller as faithfully as possible.
    
    Args:
        path (list of Configs): a list of milestones

        controller (SimRobotController or RobotInterfaceBase): the controller
            to execute the path.

        speed (float, optional): if given, changes the execution speed of the
            path.  Not valid with smoothing=None or 'ramp'.

        smoothing (str, optional): any smoothing applied to the path.  Valid
            values are:

          - None: starts / stops at each milestone, moves in linear joint-space
            paths. Trapezoidal velocity profile used.  This is most useful for
            executing paths coming from a kinematic motion planner.
          - 'linear': interpolates milestones linearly with fixed duration.  
            Constant velocity profile used.
          - 'cubic': interpolates milestones with cubic spline with fixed 
            duration.  Parabolic velocity profile used.  Starts/stops at each 
            milestone.
          - 'spline': interpolates milestones smoothly with some differenced
            velocity.
          - 'ramp': starts / stops at each milestone, moves in minimum-time / 
            minimum-acceleration paths.  Trapezoidal velocity profile used.

        activeDofs (list, optional): if not None, a list of dofs that are moved
            by the trajectory. Each entry may be an integer or a string.
    """
    if len(path)==0: return  #be tolerant of empty paths?
    if speed <= 0: raise ValueError("Speed must be positive")
    from ..control.robotinterface import RobotInterfaceBase
    from ..robotsim import SimRobotController

    if isinstance(controller,SimRobotController):
        robot_model = controller.model()
        q0 = controller.getCommandedConfig()
    elif isinstance(controller,RobotInterfaceBase):
        robot_model = controller.klamptModel()
        cq0 = controller.commandedPosition()
        if cq0[0] is None:
            cq0 = controller.sensedPosition()
        q0 = controller.configFromKlampt(cq0)
    else:
        raise ValueError("Invalid type of controller, must be SimRobotController or RobotInterfaceBase")
    if activeDofs is not None:
        indices = [robot_model.link(d).getIndex for d in activeDofs]
        liftedMilestones = []
        for m in path:
            assert(len(m)==len(indices))
            q = q0[:]
            for i,v in zip(indices,m):
                q[i] = v
            liftedMilestones.append(q)
        return execute_path(liftedMilestones,controller,speed,smoothing)

    if smoothing == None:
        if isinstance(controller,SimRobotController):
            if speed != 1.0: raise ValueError("Can't specify speed with no smoothing")
            controller.setMilestone(path[0])
            for i in range(1,len(path)):
                controller.addMilestoneLinear(path[i])
        else:
            vmax = robot_model.getVelocityLimits()
            amax = robot_model.getAccelerationLimits()
            if speed != 1.0:
                vmax = vectorops.mul(vmax,speed)
                amax = vectorops.mul(amax,speed**2)
            htraj = HermiteTrajectory()
            if q0 != path[0]:
                mpath = [q0] + path
            else:
                mpath = path
            htraj.makeMinTimeSpline(mpath,vmax=vmax,amax=amax)
            return execute_trajectory(htraj,controller)
    elif smoothing == 'linear':
        dt = 1.0/speed
        if isinstance(controller,SimRobotController):
            controller.setLinear(dt,path[0])
            for i in range(1,len(path)):
                controller.addLinear(dt,path[i])
        else:
            traj = Trajectory()
            traj.times,traj.milestones = [0],[q0]
            for i in range(len(path)):
                if i==0 and q0 == path[i]: continue  #skip first milestone
                traj.times.append(traj.times[-1]+dt)
                traj.milestones.append(path[i])
            return execute_trajectory(traj,controller)
    elif smoothing == 'cubic':
        dt = 1.0/speed
        if isinstance(controller,SimRobotController):
            zero = [0.0]*len(path[0])
            controller.setCubic(dt,path[0],zero)
            for i in range(1,len(path)):
                controller.addCubic(dt,path[i],zero)
        else:
            zero = [0.0]*controller.numJoints()
            times,milestones = [0],[q0]
            for i in range(len(path)):
                if i==0 and q0 == path[i]: continue  #skip first milestone
                times.append(times[-1]+dt)
                milestones.append(path[i])
            htraj = HermiteTrajectory(times,milestones,[zero]*len(milestones))
            return execute_trajectory(htraj,controller)
    elif smoothing == 'spline':
        dt = 1.0/speed
        times = [0]
        mpath = [q0]
        for i in range(len(path)):
            if i==0 and path[0]==q0: continue
            times.append(times[-1]+dt)
            mpath.append(path[i])
        hpath = HermiteTrajectory()
        hpath.makeSpline(Trajectory(times,mpath))
        return execute_trajectory(hpath,controller)
    elif smoothing == 'ramp':
        if isinstance(controller,SimRobotController):
            if speed != 1.0: raise ValueError("Can't specify speed with ramp smoothing")
            controller.setMilestone(path[0])
            for i in range(1,len(path)):
                controller.addMilestone(path[i])
        else:
            cv0 = controller.commandedVelocity()
            if cv0[0] == None:
                cv0 = controller.sensedVelocity()
            v0 = controller.velocityFromKlampt(cv0)
            xmin,xmax = robot_model.getJointLimits()
            vmax = robot_model.getVelocityLimits()
            amax = robot_model.getAccelerationLimits()
            if speed != 1.0:
                vmax = vectorops.mul(vmax,speed)
                amax = vectorops.mul(amax,speed**2)
            zero = [0.0]*len(q0)
            if q0 != path[0]:
                mpath = [q0] + path
                mvels = [v0] + [zero]*len(path)
            else:
                mpath = path
                mvels = [v0] + [zero]*(len(path)-1)
            zero = [0.0]*len(q0)
            htraj = HermiteTrajectory()
            htraj.makeMinTimeSpline(mpath,mvels,xmin=xmin,xmax=xmax,vmax=vmax,amax=amax)
            return execute_trajectory(htraj,controller)
    else:
        raise ValueError("Invalid smoothing method specified")


def execute_trajectory(
        trajectory: Trajectory,
        controller: Union['SimRobotController','RobotInterfaceBase'],
        speed: float = 1.0,
        smoothing: Optional[_SMOOTHING_OPTIONS2] = None,
        activeDofs: Optional[List[Union[int,str]]] = None
    ):
    """Sends a timed trajectory to a controller.

    Args:
        trajectory (Trajectory): a Trajectory, RobotTrajectory, or
            HermiteTrajectory instance.
        controller (SimRobotController or RobotInterfaceBase): the controller
            to execute the trajectory.
        speed (float, optional): modulates the speed of the path.
        smoothing (str, optional): any smoothing applied to the path.  Only
            valid for piecewise linear trajectories.  Valid values are

            * None: no smoothing, just do a piecewise linear trajectory
            * 'spline': interpolate tangents to the curve
            * 'pause': smoothly speed up and slow down

        activeDofs (list, optional): if not None, a list of dofs that are moved
            by the trajectory.  Each entry may be an integer or a string.
    """
    if len(trajectory.times)==0: return  #be tolerant of empty paths?
    if speed <= 0: raise ValueError("Speed must be positive")
    from ..control.robotinterface import RobotInterfaceBase
    from ..robotsim import SimRobotController
    if isinstance(controller,SimRobotController):
        robot_model = controller.model()
        q0 = controller.getCommandedConfig()
    elif isinstance(controller,RobotInterfaceBase):
        robot_model = controller.klamptModel()
        cq0 = controller.commandedPosition()
        if cq0[0] is None:
            cq0 = controller.sensedPosition()
        q0 = controller.configToKlampt(cq0)
    else:
        raise ValueError("Invalid type of controller, must be SimRobotController or RobotInterfaceBase")
    if activeDofs is not None:
        indices = [robot_model.link(d).getIndex for d in activeDofs]
        liftedMilestones = []
        assert not isinstance(trajectory,HermiteTrajectory),"TODO: hermite trajectory lifting"
        for m in trajectory.milestones:
            assert(len(m)==len(indices))
            q = q0[:]
            for i,v in zip(indices,m):
                q[i] = v
            liftedMilestones.append(q)
        tfull = trajectory.constructor()(trajectory.times,liftedMilestones)
        return execute_trajectory(tfull,controller,speed,smoothing)

    if isinstance(trajectory,HermiteTrajectory):
        assert smoothing == None,"Smoothing cannot be applied to hermite trajectories"
        ts = trajectory.startTime()
        n = len(q0)
        if isinstance(controller,SimRobotController):
            controller.setMilestone(trajectory.eval(ts),vectorops.mul(trajectory.deriv(ts),speed))
            n = len(trajectory.milestones[0])//2
            for i in range(1,len(trajectory.times)):
                q,v = trajectory.milestones[i][:n],trajectory.milestones[i][n:]
                controller.addCubic(q,vectorops.mul(v,speed),(trajectory.times[i]-trajectory.times[i-1])/speed)
        else:
            cv0 = controller.commandedVelocity()
            if cv0[0] is None:
                cv0 = controller.sensedVelocity()
            times,positions,velocities = [0],[controller.configFromKlampt(q0)],[cv0]
            start = 1 if trajectory.times[0]==0 else 0
            for i in range(start,len(trajectory.milestones)):
                times.append(trajectory.times[i]/speed)
                positions.append(controller.configFromKlampt(trajectory.milestones[i][:n]))
                velocities.append(controller.velocityFromKlampt(trajectory.milestones[i][n:]))
            controller.setPiecewiseCubic(times,positions,velocities)
    else:
        if smoothing == None:
            ts = trajectory.startTime()
            if isinstance(controller,SimRobotController):
                controller.setMilestone(trajectory.eval(ts))
                for i in range(1,len(trajectory.times)):
                    q = trajectory.milestones[i]
                    controller.addLinear(q,(trajectory.times[i]-trajectory.times[i-1])/speed)
            else:
                #TODO: move to start?
                times,positions = [0],[controller.configFromKlampt(q0)]
                start = 1 if 0==trajectory.times[0] else 0
                for i in range(start,len(trajectory.milestones)):
                    times.append(trajectory.times[i]/speed)
                    positions.append(controller.configFromKlampt(trajectory.milestones[i]))
                controller.setPiecewiseLinear(times,positions)
        elif smoothing == 'spline':
            t = HermiteTrajectory()
            t.makeSpline(trajectory)
            return execute_trajectory(t,controller)
        elif smoothing == 'pause':
            if isinstance(controller,SimRobotController):
                ts = trajectory.startTime()
                controller.setMilestone(trajectory.eval(ts))
                zero = [0.0]*len(trajectory.milestones[0])
                for i in range(1,len(trajectory.times)):
                    q = trajectory.milestones[i]
                    controller.addCubic(q,zero,(trajectory.times[i]-trajectory.times[i-1])/speed)
            else:
                #TODO: move to start?
                zero = [.0]*len(q0)
                t = HermiteTrajectory(trajectory.times,trajectory.milestones,[zero]*len(trajectory.milestones))
                return execute_trajectory(t,controller)
        else:
            raise ValueError("Invalid smoothing method specified")


}